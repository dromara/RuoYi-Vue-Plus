package com.wudgaby.stars.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.wudgaby.stars.domain.bo.BindGithubRequest;
import com.wudgaby.stars.domain.vo.GithubStatusVo;
import com.wudgaby.stars.service.IStarsGithubService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GitHub 账号绑定
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/stars/github")
public class StarsGithubController extends BaseController {

    private final IStarsGithubService starsGithubService;

    /**
     * 绑定 GitHub PAT
     */
    @SaCheckPermission("stars:github:bind")
    @PostMapping("/bind")
    public R<Void> bind(@RequestBody @Valid BindGithubRequest request) {
        starsGithubService.bind(LoginHelper.getUserId(), request.token());
        return R.ok();
    }

    /**
     * 解绑 GitHub 账号
     */
    @DeleteMapping("/unbind")
    public R<Void> unbind() {
        starsGithubService.unbind(LoginHelper.getUserId());
        return R.ok();
    }

    /**
     * 查询绑定状态
     */
    @GetMapping("/status")
    public R<GithubStatusVo> status() {
        return R.ok(starsGithubService.getStatus(LoginHelper.getUserId()));
    }
}
