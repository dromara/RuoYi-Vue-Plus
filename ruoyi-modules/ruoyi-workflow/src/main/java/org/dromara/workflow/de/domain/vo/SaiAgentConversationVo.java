package org.dromara.workflow.de.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 智能体对话表 sai_agent_conversation 响应 VO。
 *
 * <p>字段清单与 DDL 真源（{@code 04-backend-code/script/sql/ry_ai.sql} 7 列）一致。
 *
 * @author AIP
 */
@Data
public class SaiAgentConversationVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 智能体ID */
    private Long agentId;

    /** 用户ID */
    private Long userId;

    /** 对话ID（UUID） */
    private String conversationId;

    /** 对话标题 */
    private String title;

    /** 创建时间 */
    private Date createDt;

    /** 更新时间 */
    private Date updateDt;
}