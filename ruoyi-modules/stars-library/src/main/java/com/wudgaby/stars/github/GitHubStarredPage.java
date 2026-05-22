package com.wudgaby.stars.github;

import java.util.List;

public record GitHubStarredPage(
    List<GitHubStarredRepo> items,
    boolean hasNext,
    int nextPage
) {
}
