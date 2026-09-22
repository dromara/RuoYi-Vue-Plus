package org.dromara.de.conversation.bo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 完成对话请求 BO (回写实例运行态)
 *
 * ★ 校验：
 * - lastResponse 1-4000 字
 * - tokensUsed 0-10000000
 * - finishReason ≤ 64 字
 *
 * @author AIP
 * @since 2026-09-22
 */
@Data
public class ConversationCompleteBo {

    /** 最后回复内容 (1-4000 字) */
    @NotBlank
    @Size(max = 4000)
    private String lastResponse;

    /** 使用的 Token 数 (0-10000000) */
    @Min(0)
    @Max(10_000_000)
    private Integer tokensUsed;

    /** 结束原因 (≤64 字) */
    @Size(max = 64)
    private String finishReason;
}