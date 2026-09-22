package org.dromara.workflow.de.constants;

/**
 * 数字员工门禁常量（N-05 / N-06 / N-09 应用层校验的单一真源）。
 *
 * <p>本类是 P2 订阅门禁期的门禁常量集合。N-06 的个数上限（5）照搬 27 号 §7.3 真值。
 *
 * @author AIP
 */
public final class DeGateConstants {

    /**
     * sai_agent.rag_ids 拆出 ID 个数上限（27 号 §7.3 真值 = 5）。
     * 底座只约束长度 64，**不约束个数**；本仓在应用层强制（不升 schema）。
     */
    public static final int MAX_RAG_IDS_COUNT = 5;

    private DeGateConstants() {
        // constants holder
    }
}