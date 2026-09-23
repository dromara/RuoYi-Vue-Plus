package org.dromara.workflow.de.op.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * OP 域告警项 VO（OP-03 / OP-04：环比窗口对比派生）。
 *
 * @author AIP
 */
@Data
public class DeOpAlertVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** sai_agent_usage_stat.agent_id */
    private Long agentId;

    /**
     * 告警类型：usage-drop（上一窗口有消息、当前窗口零消息）/
     * conversation-stalled（当前窗口有消息、零对话轮次）
     */
    private String alertType;

    /** 严重度：high / medium */
    private String severity;

    /** 上一窗口消息量 */
    private Integer prevWindowMessages;

    /** 当前窗口消息量 */
    private Integer currWindowMessages;
}
