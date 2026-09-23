package org.dromara.workflow.de.op.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * OP 域运营指标 VO（OP-04：sai_agent_usage_stat 窗口聚合）。
 *
 * @author AIP
 */
@Data
public class DeOpMetricsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 统计窗口天数 */
    private Integer windowDays;

    /** 窗口起点（含） */
    private Date windowStart;

    /** 窗口内有用量记录的 Agent 数（空数据 = 0） */
    private Integer agentCount;

    /** 窗口内消息总量（sai_agent_usage_stat.message_count 求和） */
    private Long totalMessages;

    /** 窗口内对话轮次总量（sai_agent_usage_stat.conversation_count 求和） */
    private Long totalConversations;

    /** 按 Agent 分解（messageCount / conversationCount 求和） */
    private List<DeOpAgentUsageVo> byAgent;

    /**
     * OP 域按 Agent 用量分解项。
     */
    @Data
    public static class DeOpAgentUsageVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** sai_agent_usage_stat.agent_id */
        private Long agentId;

        /** 消息量 */
        private Integer messageCount;

        /** 对话轮次 */
        private Integer conversationCount;
    }
}
