package com.wudgaby.stars.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wudgaby.stars.ai.RepoEnrichmentClient;
import com.wudgaby.stars.config.StarsProperties;
import com.wudgaby.stars.domain.StarsRepo;
import com.wudgaby.stars.domain.StarsTag;
import com.wudgaby.stars.domain.StarsUserRepo;
import com.wudgaby.stars.domain.StarsUserRepoTag;
import com.wudgaby.stars.domain.ai.RepoEnrichmentResult;
import com.wudgaby.stars.enums.ClassificationSource;
import com.wudgaby.stars.enums.SummarySource;
import com.wudgaby.stars.enums.SummaryStatus;
import com.wudgaby.stars.github.GitHubApiClient;
import com.wudgaby.stars.mapper.StarsRepoMapper;
import com.wudgaby.stars.mapper.StarsTagMapper;
import com.wudgaby.stars.mapper.StarsUserRepoMapper;
import com.wudgaby.stars.mapper.StarsUserRepoTagMapper;
import com.wudgaby.stars.messaging.EnrichmentCommand;
import com.wudgaby.stars.messaging.EnrichmentProducer;
import com.wudgaby.stars.observe.StarsMetrics;
import com.wudgaby.stars.service.IStarsEnrichmentService;
import com.wudgaby.stars.service.IStarsGithubService;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.utils.IdGeneratorUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * 仓库 AI enrichment 服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class StarsEnrichmentServiceImpl implements IStarsEnrichmentService {

    private static final int MAX_AI_TAGS = 5;
    private static final int README_CACHE_HOURS = 24;

    private final StarsUserRepoMapper userRepoMapper;
    private final StarsRepoMapper repoMapper;
    private final StarsTagMapper tagMapper;
    private final StarsUserRepoTagMapper userRepoTagMapper;
    private final GitHubApiClient gitHubApiClient;
    private final IStarsGithubService githubService;
    private final RepoEnrichmentClient enrichmentClient;
    private final EnrichmentProducer enrichmentProducer;
    private final StarsProperties starsProperties;
    private final StarsMetrics starsMetrics;

    @Override
    public void requestRegenerate(Long userId, Long userRepoId) {
        StarsUserRepo userRepo = requireOwnedUserRepo(userId, userRepoId);
        updateSummaryStatus(userRepoId, SummaryStatus.PENDING);
        enrichmentProducer.enqueue(new EnrichmentCommand(userId, userRepo.getId(), userRepo.getRepoId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void process(EnrichmentCommand command) {
        starsMetrics.incrementEnrichmentRequest();
        Timer.Sample sample = Timer.start();
        updateSummaryStatus(command.userRepoId(), SummaryStatus.PROCESSING);

        try {
            StarsUserRepo userRepo = requireOwnedUserRepo(command.userId(), command.userRepoId());
            StarsRepo repo = repoMapper.selectById(command.repoId());
            if (repo == null) {
                throw new ServiceException("仓库不存在: repoId=" + command.repoId());
            }

            String readmeSnippet = getOrFetchReadme(repo, command.userId());
            RepoEnrichmentResult result = enrichmentClient.enrich(repo, readmeSnippet);
            applyEnrichment(userRepo.getId(), result);
            applyAiTags(command.userId(), userRepo.getId(), result.tags());
            updateSummaryStatus(userRepo.getId(), SummaryStatus.DONE);
            starsMetrics.incrementEnrichmentSuccess();
        } catch (Exception ex) {
            log.warn("Enrichment failed: userRepoId={}, retryCount={}, reason={}",
                command.userRepoId(), command.retryCount(), ex.getMessage());
            handleFailure(command, ex);
        } finally {
            sample.stop(starsMetrics.enrichmentTimer());
        }
    }

    private void applyEnrichment(Long userRepoId, RepoEnrichmentResult result) {
        StarsUserRepo patch = new StarsUserRepo();
        patch.setId(userRepoId);
        patch.setSummaryOneLiner(truncate(result.oneLiner(), 100));
        patch.setSummaryText(truncate(result.summary(), 500));
        patch.setCategory(StringUtils.trim(result.category()));
        patch.setClassificationSource(ClassificationSource.AI.name().toLowerCase(Locale.ROOT));
        patch.setSummarySource(SummarySource.AI.name().toLowerCase(Locale.ROOT));
        patch.setUpdateTime(LocalDateTime.now());
        userRepoMapper.updateById(patch);
    }

    private void applyAiTags(Long userId, Long userRepoId, List<String> tagNames) {
        userRepoTagMapper.delete(new LambdaQueryWrapper<StarsUserRepoTag>()
            .eq(StarsUserRepoTag::getUserRepoId, userRepoId));

        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }

        tagNames.stream()
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .distinct()
            .limit(MAX_AI_TAGS)
            .forEach(name -> linkTag(userId, userRepoId, name));
    }

    private void linkTag(Long userId, Long userRepoId, String name) {
        StarsTag tag = tagMapper.selectOne(new LambdaQueryWrapper<StarsTag>()
            .eq(StarsTag::getUserId, userId)
            .eq(StarsTag::getName, name));
        if (tag == null) {
            tag = new StarsTag();
            tag.setId(IdGeneratorUtil.nextLongId());
            tag.setUserId(userId);
            tag.setName(name);
            tag.setCreateTime(LocalDateTime.now());
            tagMapper.insert(tag);
        }

        StarsUserRepoTag link = new StarsUserRepoTag();
        link.setUserRepoId(userRepoId);
        link.setTagId(tag.getId());
        userRepoTagMapper.insert(link);
    }

    private String getOrFetchReadme(StarsRepo repo, Long userId) {
        int maxChars = starsProperties.summary().readmeMaxChars();
        LocalDateTime cachedAt = repo.getReadmeCachedAt();
        if (StringUtils.isNotBlank(repo.getReadmeSnippet())
            && cachedAt != null
            && cachedAt.isAfter(LocalDateTime.now().minusHours(README_CACHE_HOURS))) {
            return repo.getReadmeSnippet();
        }

        String token = resolveGitHubToken(userId);
        String snippet = gitHubApiClient.fetchReadmeSnippet(
            token,
            repo.getOwner(),
            repo.getRepoName(),
            maxChars);

        StarsRepo patch = new StarsRepo();
        patch.setId(repo.getId());
        patch.setReadmeSnippet(snippet);
        patch.setReadmeCachedAt(LocalDateTime.now());
        patch.setUpdateTime(LocalDateTime.now());
        repoMapper.updateById(patch);
        repo.setReadmeSnippet(snippet);
        repo.setReadmeCachedAt(patch.getReadmeCachedAt());
        return snippet;
    }

    private void handleFailure(EnrichmentCommand command, Exception ex) {
        int retryMax = starsProperties.summary().retryMax();
        if (command.retryCount() < retryMax) {
            enrichmentProducer.enqueue(new EnrichmentCommand(
                command.userId(),
                command.userRepoId(),
                command.repoId(),
                command.retryCount() + 1));
            return;
        }
        log.error("Enrichment exhausted retries: userRepoId={}", command.userRepoId(), ex);
        updateSummaryStatus(command.userRepoId(), SummaryStatus.FAILED);
        starsMetrics.incrementEnrichmentFailure();
    }

    private void updateSummaryStatus(Long userRepoId, SummaryStatus status) {
        StarsUserRepo patch = new StarsUserRepo();
        patch.setId(userRepoId);
        patch.setSummaryStatus(status.name().toLowerCase(Locale.ROOT));
        patch.setUpdateTime(LocalDateTime.now());
        userRepoMapper.updateById(patch);
    }

    private StarsUserRepo requireOwnedUserRepo(Long userId, Long userRepoId) {
        StarsUserRepo userRepo = userRepoMapper.selectById(userRepoId);
        if (userRepo == null || !userId.equals(userRepo.getUserId())) {
            throw new ServiceException("用户仓库不存在");
        }
        return userRepo;
    }

    private String resolveGitHubToken(Long userId) {
        try {
            return githubService.decryptToken(userId);
        } catch (ServiceException ex) {
            String fallback = starsProperties.github().fallbackToken();
            if (StringUtils.isNotBlank(fallback)) {
                return fallback;
            }
            throw ex;
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
