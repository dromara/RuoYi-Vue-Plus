package com.wudgaby.stars.service;

import com.wudgaby.stars.domain.vo.GithubStatusVo;

/**
 * GitHub 账号绑定服务
 */
public interface IStarsGithubService {

    /**
     * 绑定 GitHub PAT
     */
    void bind(Long userId, String token);

    /**
     * 解绑 GitHub 账号
     */
    void unbind(Long userId);

    /**
     * 查询绑定状态（不暴露 Token）
     */
    GithubStatusVo getStatus(Long userId);

    /**
     * 解密 PAT，供内部导入流程使用
     */
    String decryptToken(Long userId);
}
