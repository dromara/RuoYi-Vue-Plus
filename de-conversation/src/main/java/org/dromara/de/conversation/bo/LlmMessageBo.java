package org.dromara.de.conversation.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * LLM 内部消息请求 BO（仅供内部 LLM-ingest 端点）
 *
 * ★ Fix #3 (HIGH) + Fix #6 (MEDIUM): 内部 BO 收 role=assistant + thinking
 *   须服务间 mTLS / 共享密钥校验（占位：本仓根模块仅写接口；密钥校验待 R1 集成后加）
 *
 * @author AIP
 * @since 2026-09-22
 */
@Data
public class LlmMessageBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private Long agentId;

    @NotBlank
    @Pattern(regexp = "assistant", message = "内部端点仅允许 role=assistant")
    private String role;

    @NotBlank
    @Size(max = 4000)
    private String content;

    @Size(max = 16000)
    private String thinking;

    @Size(max = 4000)
    private String metadata;

    @Size(max = 64)
    private String finishReason;

    private Integer inputTokens;
    private Integer outputTokens;
    private Integer cacheTokens;
}