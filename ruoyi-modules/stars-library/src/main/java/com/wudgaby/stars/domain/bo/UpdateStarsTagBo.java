package com.wudgaby.stars.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 更新标签请求
 */
public record UpdateStarsTagBo(
    @NotNull(message = "标签 ID 不能为空") Long id,
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 50, message = "标签名称不能超过50个字符")
    String name,
    @Size(max = 20, message = "标签颜色不能超过20个字符")
    String color
) {
}
