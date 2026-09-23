package org.dromara.workflow.de.cv.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * CV-01 执行触发结果 VO（执行句柄）。
 *
 * @author AIP
 */
@Data
public class DeCvExecuteResultVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 触发落库的记录 ID（sai_agent_conversation_record.id） */
    private Long recordId;

    /** 会话 ID */
    private String conversationId;

    /** 执行的 Agent */
    private Long agentId;

    /** 发起用户 */
    private Long userId;

    /** 锚定流程实例（可为空 = 自由对话，CV-06） */
    private Long instanceId;

    /** 落库记录状态（3=进行中） */
    private Integer recordStatus;

    /** 执行态（触发即 running） */
    private String executionState;
}
