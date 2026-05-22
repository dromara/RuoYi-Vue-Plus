package com.wudgaby.stars.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wudgaby.stars.config.StarsProperties;
import com.wudgaby.stars.domain.StarsImportJob;
import com.wudgaby.stars.domain.StarsRepo;
import com.wudgaby.stars.domain.StarsUserRepo;
import com.wudgaby.stars.domain.vo.StarsImportJobVo;
import com.wudgaby.stars.enums.ImportJobStatus;
import com.wudgaby.stars.enums.ImportJobType;
import com.wudgaby.stars.enums.SummaryStatus;
import com.wudgaby.stars.github.GitHubApiClient;
import com.wudgaby.stars.github.GitHubRateLimitException;
import com.wudgaby.stars.github.GitHubStarredPage;
import com.wudgaby.stars.github.GitHubStarredRepo;
import com.wudgaby.stars.mapper.StarsImportJobMapper;
import com.wudgaby.stars.mapper.StarsRepoMapper;
import com.wudgaby.stars.mapper.StarsUserRepoMapper;
import com.wudgaby.stars.messaging.EnrichmentCommand;
import com.wudgaby.stars.messaging.EnrichmentProducer;
import com.wudgaby.stars.observe.StarsMetrics;
import com.wudgaby.stars.service.IStarsGithubService;
import com.wudgaby.stars.service.IStarsImportService;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.mybatis.utils.IdGeneratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

/**
 * Stars 导入任务服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class StarsImportServiceImpl implements IStarsImportService {

    private static final String IMPORT_SOURCE_SELF = "self";

    private final StarsImportJobMapper jobMapper;
    private final StarsRepoMapper repoMapper;
    private final StarsUserRepoMapper userRepoMapper;
    private final GitHubApiClient gitHubApiClient;
    private final IStarsGithubService githubService;
    private final StarsProperties starsProperties;
    private final EnrichmentProducer enrichmentProducer;
    private final StarsMetrics starsMetrics;

    @Lazy
    @Autowired
    private StarsImportServiceImpl self;

    @Override
    public Long startSelfSync(Long userId, Integer limit) {
        int importLimit = resolveImportLimit(limit);
        String token = githubService.decryptToken(userId);
        StarsImportJob job = createJob(userId, ImportJobType.SELF_SYNC, null, importLimit);
        self.runSelfSync(userId, job.getId(), token, importLimit);
        return job.getId();
    }

    @Override
    public Long startImportUser(Long userId, String login, Integer limit) {
        if (StringUtils.isBlank(login)) {
            throw new ServiceException("GitHub 用户名不能为空");
        }
        int importLimit = resolveImportLimit(limit);
        String normalizedLogin = login.trim();
        String token = resolveTokenForImport(userId);
        StarsImportJob job = createJob(userId, ImportJobType.IMPORT_USER, normalizedLogin, importLimit);
        self.runImportUser(userId, job.getId(), token, normalizedLogin, importLimit);
        return job.getId();
    }

    @Override
    public TableDataInfo<StarsImportJobVo> queryPageList(Long userId, PageQuery pageQuery) {
        LambdaQueryWrapper<StarsImportJob> lqw = Wrappers.lambdaQuery();
        lqw.eq(StarsImportJob::getUserId, userId);
        lqw.orderByDesc(StarsImportJob::getStartTime);
        Page<StarsImportJobVo> page = jobMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public StarsImportJobVo queryById(Long userId, Long jobId) {
        StarsImportJob job = requireOwnedJob(userId, jobId);
        return jobMapper.selectVoById(job.getId());
    }

    @Async
    public void runSelfSync(Long userId, Long jobId, String token, int importLimit) {
        executeImport(userId, jobId, token, null, IMPORT_SOURCE_SELF, importLimit);
    }

    @Async
    public void runImportUser(Long userId, Long jobId, String token, String login, int importLimit) {
        executeImport(userId, jobId, token, login, login, importLimit);
    }

    private void executeImport(
        Long userId,
        Long jobId,
        String token,
        String githubLogin,
        String importSource,
        int importLimit) {
        starsMetrics.incrementImportJobStarted();
        Timer.Sample sample = Timer.start();
        markRunning(jobId);
        int page = 1;
        int processed = 0;
        int failed = 0;
        int fetched = 0;

        try {
            fetchLoop:
            while (fetched < importLimit) {
                GitHubStarredPage starredPage = fetchStarredWithRetry(jobId, token, githubLogin, page);
                for (GitHubStarredRepo item : starredPage.items()) {
                    if (fetched >= importLimit) {
                        break fetchLoop;
                    }
                    fetched++;
                    try {
                        upsertRepo(userId, item, importSource);
                        processed++;
                    } catch (Exception ex) {
                        failed++;
                        log.warn("Import repo failed: userId={}, jobId={}, fullName={}, reason={}",
                            userId, jobId, item.fullName(), ex.getMessage());
                    }
                    updateProgress(jobId, importLimit, processed, failed);
                }
                if (!starredPage.hasNext() || fetched >= importLimit) {
                    break;
                }
                page = starredPage.nextPage();
                sleepInterval();
            }
            finalizeJob(jobId, importLimit, processed, failed, null);
        } catch (WebClientResponseException.NotFound ex) {
            finalizeJob(jobId, importLimit, processed, failed, "GitHub 用户不存在或 Stars 不可访问");
        } catch (ServiceException ex) {
            finalizeJob(jobId, importLimit, processed, failed, ex.getMessage());
        } catch (Exception ex) {
            log.error("Import job failed: jobId={}", jobId, ex);
            finalizeJob(jobId, importLimit, processed, failed, ex.getMessage());
        } finally {
            sample.stop(starsMetrics.importJobTimer());
        }
    }

    private GitHubStarredPage fetchStarredWithRetry(Long jobId, String token, String githubLogin, int page) {
        while (true) {
            try {
                return gitHubApiClient.fetchUserStarred(token, githubLogin, page);
            } catch (GitHubRateLimitException ex) {
                updateRateLimitMessage(jobId, ex.getMessage());
                sleepSeconds(ex.getRetryAfterSeconds());
            }
        }
    }

    private void upsertRepo(Long userId, GitHubStarredRepo gh, String importSource) {
        if (gh == null || StringUtils.isBlank(gh.fullName())) {
            throw new ServiceException("仓库 full_name 为空");
        }
        StarsRepo repo = upsertGlobalRepo(gh);
        StarsUserRepo existing = userRepoMapper.selectByUserAndRepo(userId, repo.getId());
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            StarsUserRepo userRepo = new StarsUserRepo();
            userRepo.setId(IdGeneratorUtil.nextLongId());
            userRepo.setUserId(userId);
            userRepo.setRepoId(repo.getId());
            userRepo.setImportSource(importSource);
            userRepo.setSummaryStatus(SummaryStatus.PENDING.name().toLowerCase(Locale.ROOT));
            userRepo.setImportTime(now);
            userRepo.setUpdateTime(now);
            userRepoMapper.insert(userRepo);
            enrichmentProducer.enqueue(new EnrichmentCommand(userId, userRepo.getId(), repo.getId()));
            return;
        }
        existing.setImportTime(now);
        existing.setUpdateTime(now);
        userRepoMapper.updateById(existing);
    }

    private StarsRepo upsertGlobalRepo(GitHubStarredRepo gh) {
        StarsRepo existing = repoMapper.selectOne(new LambdaQueryWrapper<StarsRepo>()
            .eq(StarsRepo::getFullName, gh.fullName()));
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            StarsRepo repo = new StarsRepo();
            repo.setId(IdGeneratorUtil.nextLongId());
            fillRepoFromGitHub(repo, gh);
            repo.setCreateTime(now);
            repo.setUpdateTime(now);
            repoMapper.insert(repo);
            return repo;
        }
        fillRepoFromGitHub(existing, gh);
        existing.setUpdateTime(now);
        repoMapper.updateById(existing);
        return existing;
    }

    private void fillRepoFromGitHub(StarsRepo repo, GitHubStarredRepo gh) {
        repo.setFullName(gh.fullName());
        repo.setOwner(gh.owner());
        repo.setRepoName(gh.name());
        repo.setDescription(gh.description());
        repo.setLanguage(gh.language());
        repo.setStargazersCount(gh.stargazersCount());
        repo.setHtmlUrl(gh.htmlUrl());
        repo.setGithubUpdatedAt(toLocalDateTime(gh.updatedAt()));
    }

    private String resolveTokenForImport(Long userId) {
        try {
            return githubService.decryptToken(userId);
        } catch (ServiceException ex) {
            String fallback = starsProperties.github().fallbackToken();
            if (StringUtils.isNotBlank(fallback)) {
                return fallback;
            }
            throw new ServiceException("未绑定 GitHub 且未配置 stars.github.fallback-token");
        }
    }

    private int resolveImportLimit(Integer requested) {
        StarsProperties.Import importConfig = starsProperties.importConfig();
        int limit = requested != null ? requested : importConfig.defaultLimit();
        if (limit < 1) {
            throw new ServiceException("导入数量至少为 1");
        }
        if (limit > importConfig.maxLimit()) {
            throw new ServiceException("导入数量不能超过 " + importConfig.maxLimit());
        }
        return limit;
    }

    private StarsImportJob createJob(Long userId, ImportJobType jobType, String sourceLogin, int importLimit) {
        LocalDateTime now = LocalDateTime.now();
        StarsImportJob job = new StarsImportJob();
        job.setId(IdGeneratorUtil.nextLongId());
        job.setUserId(userId);
        job.setJobType(jobType.name().toLowerCase(Locale.ROOT));
        job.setSourceLogin(sourceLogin);
        job.setImportLimit(importLimit);
        job.setStatus(ImportJobStatus.PENDING.name().toLowerCase(Locale.ROOT));
        job.setTotalCount(importLimit);
        job.setProcessedCount(0);
        job.setFailedCount(0);
        job.setStartTime(now);
        jobMapper.insert(job);
        return job;
    }

    private void markRunning(Long jobId) {
        StarsImportJob patch = new StarsImportJob();
        patch.setId(jobId);
        patch.setStatus(ImportJobStatus.RUNNING.name().toLowerCase(Locale.ROOT));
        patch.setErrorMessage(null);
        jobMapper.updateById(patch);
    }

    private void updateProgress(Long jobId, int importLimit, int processed, int failed) {
        StarsImportJob patch = new StarsImportJob();
        patch.setId(jobId);
        patch.setProcessedCount(processed);
        patch.setFailedCount(failed);
        patch.setTotalCount(importLimit);
        jobMapper.updateById(patch);
    }

    private void updateRateLimitMessage(Long jobId, String message) {
        StarsImportJob patch = new StarsImportJob();
        patch.setId(jobId);
        patch.setStatus(ImportJobStatus.RUNNING.name().toLowerCase(Locale.ROOT));
        patch.setErrorMessage(message);
        jobMapper.updateById(patch);
    }

    private void finalizeJob(Long jobId, int importLimit, int processed, int failed, String errorMessage) {
        StarsImportJob job = jobMapper.selectById(jobId);
        if (job == null) {
            return;
        }
        job.setProcessedCount(processed);
        job.setFailedCount(failed);
        if (job.getImportLimit() == null) {
            job.setImportLimit(importLimit);
        }
        int actualTotal = processed + failed;
        job.setTotalCount(actualTotal < importLimit ? actualTotal : importLimit);
        job.setEndTime(LocalDateTime.now());
        job.setErrorMessage(errorMessage);

        if (StringUtils.isNotBlank(errorMessage) && processed == 0) {
            job.setStatus(ImportJobStatus.FAILED.name().toLowerCase(Locale.ROOT));
            starsMetrics.incrementImportJobFailed();
        } else if (failed > 0 || StringUtils.isNotBlank(errorMessage)) {
            job.setStatus(ImportJobStatus.PARTIAL.name().toLowerCase(Locale.ROOT));
            starsMetrics.incrementImportJobPartial();
        } else {
            job.setStatus(ImportJobStatus.DONE.name().toLowerCase(Locale.ROOT));
            starsMetrics.incrementImportJobDone();
        }
        jobMapper.updateById(job);
    }

    private StarsImportJob requireOwnedJob(Long userId, Long jobId) {
        StarsImportJob job = jobMapper.selectById(jobId);
        if (job == null || !userId.equals(job.getUserId())) {
            throw new ServiceException("导入任务不存在");
        }
        return job;
    }

    private void sleepInterval() {
        sleepMillis(starsProperties.github().requestIntervalMs());
    }

    private void sleepSeconds(long seconds) {
        sleepMillis(Math.max(seconds, 1L) * 1000L);
    }

    private void sleepMillis(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceException("导入任务被中断");
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
