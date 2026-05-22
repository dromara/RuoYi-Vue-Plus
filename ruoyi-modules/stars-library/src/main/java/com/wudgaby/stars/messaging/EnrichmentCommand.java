package com.wudgaby.stars.messaging;

/**
 * 仓库 AI enrichment 命令
 */
public record EnrichmentCommand(
    Long userId,
    Long userRepoId,
    Long repoId,
    int retryCount
) {

    public EnrichmentCommand(Long userId, Long userRepoId, Long repoId) {
        this(userId, userRepoId, repoId, 0);
    }
}
