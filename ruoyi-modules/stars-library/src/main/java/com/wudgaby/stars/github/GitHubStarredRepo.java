package com.wudgaby.stars.github;

import java.time.Instant;

public record GitHubStarredRepo(
    String fullName,
    String owner,
    String name,
    String description,
    String language,
    int stargazersCount,
    String htmlUrl,
    Instant updatedAt
) {
}
