package org.dromara.de.conversation.bo;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建消息请求 BO（caller 端 = 仅 role=user）
 *
 * ★ Fix #3 (HIGH): caller 端只许 role=user；assistant 由内部 LLM-ingest 端点收
 * ★ Fix #5 (MEDIUM): metadata 简化校验（≤4000 字 + 简易 JSON 格式校验）
 * ★ Fix #6 (MEDIUM): caller 端不收 thinking（仅内部 LLM-ingest 收）
 *
 * @author AIP
 * @since 2026-09-22
 */
@Data
public class MessageCreateBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数字员工 ID */
    @NotNull
    private Long agentId;

    /** 角色（caller 端仅 user；assistant 走内部 /llm-message）*/
    @NotBlank
    @Pattern(regexp = "user", message = "caller 端只允许 role=user；assistant 请用 /llm-message")
    private String role;

    /** 消息内容 (1-4000 字) */
    @NotBlank
    @Size(max = 4000)
    private String content;

    /** 元数据 JSON（≤4000 字；简易校验：含 trace_id / model / finish_reason 等键） */
    @Size(max = 4000)
    private String metadata;

    /**
     * Fix #5 (MEDIUM) 简化版：metadata 必须是 {} 包围的 JSON（首尾大括号）
     * 完整深度/键数校验在 LLM-ingest 端点
     */
    @AssertTrue(message = "metadata 须为 JSON 对象（首尾大括号）")
    public boolean isMetadataJsonShape() {
        if (metadata == null || metadata.isBlank()) return true;
        String trimmed = metadata.trim();
        return trimmed.startsWith("{") && trimmed.endsWith("}");
    }
}