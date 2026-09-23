package org.dromara.workflow.de.cv.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * CV-01 执行触发请求 BO。
 *
 * @author AIP
 */
@Data
public class DeCvExecuteBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 目标会话 ID（sai_agent_conversation.conversation_id，必填） */
    @NotBlank
    private String conversationId;

    /** 执行输入内容（落 sai_agent_conversation_record.content，必填） */
    @NotBlank
    private String content;
}
