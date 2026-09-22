package org.dromara.workflow.de.br.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * BR-06 记忆适用范围请求。
 *
 * <p>落点：{@code sai_agent_conversation_record.metadata}（LONGTEXT JSON）。
 */
@Data
public class DeConvRecordMetaBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** sai_agent_conversation_record.id */
    @NotNull(message = "recordId 不能为空")
    private Long recordId;

    /** 记忆范围：{@code PERSONAL / TEAM / PROJECT} */
    @NotBlank(message = "scope 不能为空")
    private String scope;

    /** 待写入的 metadata JSON（将被注入 {@code de.scope} 键后再持久化） */
    @NotBlank(message = "metadataJson 不能为空")
    private String metadataJson;
}
