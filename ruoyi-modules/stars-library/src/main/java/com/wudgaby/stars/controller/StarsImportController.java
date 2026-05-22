package com.wudgaby.stars.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.wudgaby.stars.domain.bo.StartImportBo;
import com.wudgaby.stars.domain.vo.StarsImportJobVo;
import com.wudgaby.stars.service.IStarsImportService;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stars 导入任务
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/stars/import")
public class StarsImportController extends BaseController {

    private final IStarsImportService starsImportService;

    /**
     * 同步当前用户 GitHub Stars
     */
    @SaCheckPermission("stars:repo:import")
    @PostMapping("/self")
    public R<Long> syncSelf(@RequestBody(required = false) @Validated StartImportBo body) {
        Integer limit = body != null ? body.getLimit() : null;
        Long jobId = starsImportService.startSelfSync(LoginHelper.getUserId(), limit);
        return R.ok(jobId);
    }

    /**
     * 导入指定 GitHub 用户的公开 Stars
     */
    @SaCheckPermission("stars:repo:import")
    @PostMapping("/user/{login}")
    public R<Long> importUser(
        @PathVariable String login,
        @RequestBody(required = false) @Validated StartImportBo body) {
        Integer limit = body != null ? body.getLimit() : null;
        Long jobId = starsImportService.startImportUser(LoginHelper.getUserId(), login, limit);
        return R.ok(jobId);
    }

    /**
     * 分页查询导入任务
     */
    @GetMapping("/jobs")
    public TableDataInfo<StarsImportJobVo> list(PageQuery pageQuery) {
        return starsImportService.queryPageList(LoginHelper.getUserId(), pageQuery);
    }

    /**
     * 查询导入任务详情
     */
    @GetMapping("/jobs/{id}")
    public R<StarsImportJobVo> getJob(@PathVariable Long id) {
        return R.ok(starsImportService.queryById(LoginHelper.getUserId(), id));
    }
}
