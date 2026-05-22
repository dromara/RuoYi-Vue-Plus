package com.wudgaby.stars.domain.bo;

import java.util.List;

/**
 * 更新用户仓库请求
 */
public record UpdateStarsRepoBo(
    String note,
    String summaryOneLiner,
    String summaryText,
    String category,
    List<Long> tagIds
) {
}
