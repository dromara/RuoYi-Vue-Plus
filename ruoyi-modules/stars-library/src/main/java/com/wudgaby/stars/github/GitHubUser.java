package com.wudgaby.stars.github;

/**
 * GitHub 当前认证用户（GET /user）
 */
public record GitHubUser(String login, String scope) {
}
