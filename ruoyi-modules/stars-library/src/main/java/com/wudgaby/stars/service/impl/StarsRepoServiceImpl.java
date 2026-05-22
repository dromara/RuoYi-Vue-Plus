package com.wudgaby.stars.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wudgaby.stars.domain.StarsTag;
import com.wudgaby.stars.domain.StarsUserRepo;
import com.wudgaby.stars.domain.StarsUserRepoTag;
import com.wudgaby.stars.domain.bo.BatchTagsRequest;
import com.wudgaby.stars.domain.bo.StarsRepoQueryBo;
import com.wudgaby.stars.domain.bo.UpdateStarsRepoBo;
import com.wudgaby.stars.domain.vo.StarsRepoCardRow;
import com.wudgaby.stars.domain.vo.StarsRepoCardVo;
import com.wudgaby.stars.domain.vo.StarsRepoDetailVo;
import com.wudgaby.stars.enums.ClassificationSource;
import com.wudgaby.stars.enums.SummarySource;
import com.wudgaby.stars.mapper.StarsTagMapper;
import com.wudgaby.stars.mapper.StarsUserRepoMapper;
import com.wudgaby.stars.mapper.StarsUserRepoTagMapper;
import com.wudgaby.stars.service.IStarsRepoService;
import com.wudgaby.stars.support.StarsDeepLinkBuilder;
import com.wudgaby.stars.support.StarsTagCsvParser;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * 用户仓库查询与编辑服务实现
 */
@RequiredArgsConstructor
@Service
public class StarsRepoServiceImpl implements IStarsRepoService {

    private final StarsUserRepoMapper userRepoMapper;
    private final StarsTagMapper tagMapper;
    private final StarsUserRepoTagMapper userRepoTagMapper;
    private final StarsDeepLinkBuilder deepLinkBuilder;

    @Override
    public TableDataInfo<StarsRepoCardVo> queryPageList(Long userId, StarsRepoQueryBo query, PageQuery pageQuery) {
        StarsRepoQueryBo effectiveQuery = query == null ? new StarsRepoQueryBo() : query;
        effectiveQuery.setUserId(userId);

        Page<StarsRepoCardRow> page = userRepoMapper.selectRepoCardPage(pageQuery.build(), effectiveQuery);
        List<StarsRepoCardVo> rows = page.getRecords().stream().map(this::toCardVo).toList();
        return new TableDataInfo<>(rows, page.getTotal());
    }

    @Override
    public StarsRepoDetailVo queryById(Long userId, Long userRepoId) {
        StarsRepoCardRow row = userRepoMapper.selectRepoDetail(userId, userRepoId);
        if (row == null) {
            throw new ServiceException("用户仓库不存在");
        }
        return toDetailVo(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long userRepoId, UpdateStarsRepoBo request) {
        requireOwnedUserRepo(userId, userRepoId);
        if (request == null) {
            return;
        }

        StarsUserRepo patch = new StarsUserRepo();
        patch.setId(userRepoId);
        boolean changed = false;

        if (request.note() != null) {
            patch.setNote(truncate(request.note(), 500));
            changed = true;
        }
        if (request.summaryOneLiner() != null) {
            patch.setSummaryOneLiner(truncate(request.summaryOneLiner(), 100));
            patch.setSummarySource(summaryManualSource());
            patch.setSummaryStatus("manual");
            changed = true;
        }
        if (request.summaryText() != null) {
            patch.setSummaryText(truncate(request.summaryText(), 500));
            patch.setSummarySource(summaryManualSource());
            patch.setSummaryStatus("manual");
            changed = true;
        }
        if (request.category() != null) {
            patch.setCategory(StringUtils.trim(request.category()));
            patch.setClassificationSource(classificationManualSource());
            changed = true;
        }

        if (changed) {
            patch.setUpdateTime(LocalDateTime.now());
            userRepoMapper.updateById(patch);
        }

        if (request.tagIds() != null) {
            replaceTags(userId, request.tagIds(), userRepoId);
        }

        if (!changed && request.tagIds() == null) {
            throw new ServiceException("没有可更新的字段");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAssignTags(Long userId, BatchTagsRequest request) {
        for (Long userRepoId : request.userRepoIds()) {
            requireOwnedUserRepo(userId, userRepoId);
        }
        for (Long tagId : request.tagIds()) {
            requireOwnedTag(userId, tagId);
        }

        for (Long userRepoId : request.userRepoIds()) {
            for (Long tagId : request.tagIds()) {
                linkTagIfAbsent(userRepoId, tagId);
            }
        }
    }

    private StarsRepoCardVo toCardVo(StarsRepoCardRow row) {
        StarsRepoCardVo vo = new StarsRepoCardVo();
        vo.setId(row.getId());
        vo.setFullName(row.getFullName());
        vo.setOwner(row.getOwner());
        vo.setRepoName(row.getRepoName());
        vo.setLanguage(row.getLanguage());
        vo.setStargazersCount(row.getStargazersCount());
        vo.setCategory(row.getCategory());
        vo.setTags(StarsTagCsvParser.parseNames(row.getTagNamesCsv()));
        vo.setSummaryOneLiner(row.getSummaryOneLiner());
        vo.setSummaryText(row.getSummaryText());
        vo.setSummaryStatus(row.getSummaryStatus());
        vo.setNote(row.getNote());
        vo.setImportSource(row.getImportSource());
        vo.setGithubUrl(deepLinkBuilder.githubUrl(row.getOwner(), row.getRepoName(), row.getHtmlUrl()));
        vo.setZreadUrl(deepLinkBuilder.zreadUrl(row.getOwner(), row.getRepoName()));
        vo.setDeepwikiUrl(deepLinkBuilder.deepwikiUrl(row.getOwner(), row.getRepoName()));
        return vo;
    }

    private StarsRepoDetailVo toDetailVo(StarsRepoCardRow row) {
        StarsRepoDetailVo vo = new StarsRepoDetailVo();
        StarsRepoCardVo card = toCardVo(row);
        vo.setId(card.getId());
        vo.setFullName(card.getFullName());
        vo.setOwner(card.getOwner());
        vo.setRepoName(card.getRepoName());
        vo.setLanguage(card.getLanguage());
        vo.setStargazersCount(card.getStargazersCount());
        vo.setCategory(card.getCategory());
        vo.setTags(card.getTags());
        vo.setSummaryOneLiner(card.getSummaryOneLiner());
        vo.setSummaryText(card.getSummaryText());
        vo.setSummaryStatus(card.getSummaryStatus());
        vo.setNote(card.getNote());
        vo.setImportSource(card.getImportSource());
        vo.setGithubUrl(card.getGithubUrl());
        vo.setZreadUrl(card.getZreadUrl());
        vo.setDeepwikiUrl(card.getDeepwikiUrl());
        vo.setRepoId(row.getRepoId());
        vo.setDescription(row.getDescription());
        vo.setReadmeSnippet(row.getReadmeSnippet());
        vo.setClassificationSource(row.getClassificationSource());
        vo.setSummarySource(row.getSummarySource());
        vo.setImportTime(row.getImportTime());
        vo.setUpdateTime(row.getUpdateTime());
        vo.setTagIds(StarsTagCsvParser.parseIds(row.getTagIdsCsv()));
        return vo;
    }

    private void replaceTags(Long userId, List<Long> tagIds, Long userRepoId) {
        userRepoTagMapper.delete(new LambdaQueryWrapper<StarsUserRepoTag>()
            .eq(StarsUserRepoTag::getUserRepoId, userRepoId));

        if (tagIds.isEmpty()) {
            return;
        }

        for (Long tagId : tagIds) {
            requireOwnedTag(userId, tagId);
            linkTagIfAbsent(userRepoId, tagId);
        }
    }

    private void linkTagIfAbsent(Long userRepoId, Long tagId) {
        Long count = userRepoTagMapper.selectCount(new LambdaQueryWrapper<StarsUserRepoTag>()
            .eq(StarsUserRepoTag::getUserRepoId, userRepoId)
            .eq(StarsUserRepoTag::getTagId, tagId));
        if (count != null && count > 0) {
            return;
        }

        StarsUserRepoTag link = new StarsUserRepoTag();
        link.setUserRepoId(userRepoId);
        link.setTagId(tagId);
        userRepoTagMapper.insert(link);
    }

    private StarsUserRepo requireOwnedUserRepo(Long userId, Long userRepoId) {
        StarsUserRepo userRepo = userRepoMapper.selectById(userRepoId);
        if (userRepo == null || !userId.equals(userRepo.getUserId())) {
            throw new ServiceException("用户仓库不存在");
        }
        return userRepo;
    }

    private StarsTag requireOwnedTag(Long userId, Long tagId) {
        StarsTag tag = tagMapper.selectById(tagId);
        if (tag == null || !userId.equals(tag.getUserId())) {
            throw new ServiceException("标签不存在");
        }
        return tag;
    }

    private static String summaryManualSource() {
        return SummarySource.MANUAL.name().toLowerCase(Locale.ROOT);
    }

    private static String classificationManualSource() {
        return ClassificationSource.MANUAL.name().toLowerCase(Locale.ROOT);
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
