package org.dromara.workflow.de.op.service;

import org.dromara.workflow.de.op.domain.vo.DeOpAlertVo;
import org.dromara.workflow.de.op.domain.vo.DeOpConfigVo;
import org.dromara.workflow.de.op.domain.vo.DeOpMetricsVo;

import java.util.List;

/**
 * 运营与调度（OP）服务接口 —— 29 号 §五 OP 域取 3 端点切片。
 *
 * <p>落点真源：sai_agent_usage_stat（message_count / conversation_count /
 * stat_date / agent_id / user_id）。底座无业务完成率 / 返工率 / 交接等待时长位
 * （29 号 OP-04 改写口径），本域只出用量侧指标，不以运行数冒充业务成果。
 *
 * @author AIP
 */
public interface IDeOpService {

    /**
     * OP-04 运营指标：窗口内 message_count / conversation_count 聚合
     * （总量 + 按 Agent 分解；空数据明确返回零值与空分解）。
     */
    DeOpMetricsVo metrics(Long agentId, Long userId, Integer days);

    /**
     * OP-03 / OP-04 告警：当前窗口与上一窗口环比 ——
     * 有用量 → 零用量 = usage-drop；有消息 → 零对话轮次 = conversation-stalled。
     */
    List<DeOpAlertVo> alerts(Integer days);

    /**
     * OP-05 / OP-06 调度口径读：统计维度 + 表内实数据窗口（min/max stat_date）。
     */
    DeOpConfigVo config();
}
