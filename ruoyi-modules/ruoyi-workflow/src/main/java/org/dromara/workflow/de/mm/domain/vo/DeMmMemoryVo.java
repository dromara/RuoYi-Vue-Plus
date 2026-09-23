package org.dromara.workflow.de.mm.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * MM 域记忆记录项 VO（sai_agent_conversation_record 单行投射）。
 *
 * @author AIP
 */
@Data
public class DeMmMemoryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** sai_agent_conversation_record.id */
    private Long recordId;

    /** sai_agent_conversation_record.agent_id */
    private Long agentId;

    /** sai_agent_conversation_record.user_id（MM-07 归属） */
    private Long userId;

    /** sai_agent_conversation_record.conversation_id */
    private String conversationId;

    /** user / assistant */
    private String role;

    /** 消息内容（MM-04 经验来源） */
    private String content;

    /** 消息扩展元数据 JSON（MM-01 记忆范围 / MM-03 本次使用记忆的载荷） */
    private String metadata;

    /** 1=成功 2=失败 3=进行中 */
    private Integer status;
}
