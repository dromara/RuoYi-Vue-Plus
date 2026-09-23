package org.dromara.workflow.de.cv.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * CV 域会话项 VO（sai_agent_conversation 单行投射）。
 *
 * @author AIP
 */
@Data
public class DeCvSessionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** sai_agent_conversation.conversation_id（UUID） */
    private String conversationId;

    /** sai_agent_conversation.agent_id */
    private Long agentId;

    /** sai_agent_conversation.user_id */
    private Long userId;

    /** sai_agent_conversation.instance_id（锚定流程实例，D-009 列） */
    private Long instanceId;

    /** sai_agent_conversation.title */
    private String title;

    /** sai_agent_conversation.create_dt */
    private Date createDt;

    /** sai_agent_conversation.update_dt */
    private Date updateDt;
}
