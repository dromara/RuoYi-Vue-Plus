package com.wudgaby.stars.domain.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DeepSeek 仓库 enrichment 结构化输出
 */
public record RepoEnrichmentResult(
    @JsonProperty("one_liner") String oneLiner,
    String summary,
    String category,
    List<String> tags
) {
}
