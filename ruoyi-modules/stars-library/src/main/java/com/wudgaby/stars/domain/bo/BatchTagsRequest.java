package com.wudgaby.stars.domain.bo;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 批量打标请求
 */
public record BatchTagsRequest(
    @NotEmpty(message = "仓库列表不能为空") List<Long> userRepoIds,
    @NotEmpty(message = "标签列表不能为空") List<Long> tagIds
) {
}
