package com.wudgaby.stars.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.wudgaby.stars.domain.bo.BatchTagsRequest;
import com.wudgaby.stars.domain.bo.StarsRepoQueryBo;
import com.wudgaby.stars.domain.bo.UpdateStarsRepoBo;
import com.wudgaby.stars.domain.vo.StarsRepoCardVo;
import com.wudgaby.stars.domain.vo.StarsRepoDetailVo;
import com.wudgaby.stars.service.IStarsEnrichmentService;
import com.wudgaby.stars.service.IStarsRepoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Stars 仓库
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/stars/repos")
public class StarsRepoController extends BaseController {

    private final IStarsRepoService starsRepoService;
    private final IStarsEnrichmentService enrichmentService;

    /**
     * 分页查询用户仓库列表
     */
    @SaCheckPermission("stars:repo:list")
    @GetMapping
    public TableDataInfo<StarsRepoCardVo> list(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String tagIds,
        @RequestParam(required = false) String importSource,
        @RequestParam(required = false) String summaryStatus,
        @RequestParam(required = false) String orderBy,
        PageQuery pageQuery) {
        StarsRepoQueryBo query = new StarsRepoQueryBo();
        query.setKeyword(StringUtils.trim(keyword));
        query.setCategory(StringUtils.trim(category));
        query.setImportSource(StringUtils.trim(importSource));
        query.setSummaryStatus(StringUtils.trim(summaryStatus));
        query.setOrderBy(StringUtils.trim(orderBy));
        applyTagIds(query, tagIds);
        return starsRepoService.queryPageList(LoginHelper.getUserId(), query, pageQuery);
    }

    /**
     * 查询仓库详情
     */
    @SaCheckPermission("stars:repo:list")
    @GetMapping("/{id}")
    public R<StarsRepoDetailVo> getInfo(@PathVariable Long id) {
        return R.ok(starsRepoService.queryById(LoginHelper.getUserId(), id));
    }

    /**
     * 更新备注、概述、分类与标签
     */
    @SaCheckPermission("stars:repo:edit")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody UpdateStarsRepoBo request) {
        starsRepoService.update(LoginHelper.getUserId(), id, request);
        return R.ok();
    }

    /**
     * 批量为仓库追加标签
     */
    @SaCheckPermission("stars:repo:edit")
    @PostMapping("/batch-tags")
    public R<Void> batchTags(@RequestBody @Valid BatchTagsRequest request) {
        starsRepoService.batchAssignTags(LoginHelper.getUserId(), request);
        return R.ok();
    }

    /**
     * 重新生成仓库 AI 概述与分类
     */
    @SaCheckPermission("stars:repo:edit")
    @PostMapping("/{id}/regenerate-summary")
    public R<Void> regenerateSummary(@PathVariable Long id) {
        enrichmentService.requestRegenerate(LoginHelper.getUserId(), id);
        return R.ok();
    }

    private static void applyTagIds(StarsRepoQueryBo query, String tagIds) {
        if (query == null || StringUtils.isBlank(tagIds)) {
            return;
        }
        List<Long> parsed = Arrays.stream(tagIds.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .map(Long::valueOf)
            .toList();
        query.setTagIds(parsed);
    }

}
