package com.wudgaby.stars.github;

public class GitHubRateLimitException extends RuntimeException {

    private final long retryAfterSeconds;

    public GitHubRateLimitException(long retryAfterSeconds) {
        super("GitHub API rate limit exceeded, retry after " + retryAfterSeconds + " seconds");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
