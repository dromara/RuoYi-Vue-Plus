package com.wudgaby.stars.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.wudgaby.stars.domain.bo.CreateStarsTagBo;
import com.wudgaby.stars.domain.bo.UpdateStarsTagBo;
import com.wudgaby.stars.domain.vo.StarsTagVo;
import com.wudgaby.stars.service.IStarsTagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Stars 标签
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/stars/tags")
public class StarsTagController extends BaseController {

    private final IStarsTagService starsTagService;

    /**
     * 查询当前用户全部标签
     */
    @SaCheckPermission("stars:repo:list")
    @GetMapping
    public R<List<StarsTagVo>> list() {
        return R.ok(starsTagService.listByUser(LoginHelper.getUserId()));
    }

    /**
     * 创建标签
     */
    @SaCheckPermission("stars:tag:edit")
    @PostMapping
    public R<StarsTagVo> create(@RequestBody @Valid CreateStarsTagBo request) {
        return R.ok(starsTagService.create(LoginHelper.getUserId(), request));
    }

    /**
     * 更新标签
     */
    @SaCheckPermission("stars:tag:edit")
    @PutMapping
    public R<StarsTagVo> update(@RequestBody @Valid UpdateStarsTagBo request) {
        return R.ok(starsTagService.update(LoginHelper.getUserId(), request));
    }

    /**
     * 删除标签
     */
    @SaCheckPermission("stars:tag:edit")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        starsTagService.delete(LoginHelper.getUserId(), id);
        return R.ok();
    }

}
