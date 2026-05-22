package com.wudgaby.stars.domain.bo;

import jakarta.validation.constraints.NotBlank;

/**
 * GitHub PAT 绑定请求
 */
public record BindGithubRequest(@NotBlank(message = "GitHub Token 不能为空") String token) {
}
