package org.dromara.workflow.de.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 数字员工绑定响应。
 *
 * <p>对应 GET /de/binding/{instanceId} 响应体。
 *
 * <p>字段落点（28 号 §一 白名单 + 读 JOIN sai_agent）：
 * <ul>
 *   <li>instanceId → flow_instance_biz_ext.instance_id</li>
 *   <li>businessCode → flow_instance_biz_ext.business_code</li>
 *   <li>agentId → flow_instance_biz_ext.business_id（字符串形式的 sai_agent.id）</li>
 *   <li>agentName → sai_agent.name（JOIN 读回）</li>
 *   <li>title → sai_agent_conversation.title（**AU-09 派生字段 — 只读**）</li>
 * </ul>
 *
 * <p><b>AU-09 「历史对话摘要」派生（D-002 已登记）</b>：{@code title} 是 AU-09 缺口
 * 「**历史对话摘要位无**」的应用层派生 —— 底座 DDL 字段真值 = {@code sai_agent_conversation.title VARCHAR(255)}
 * （{@code 04-backend-code/script/sql/ry_ai.sql:172}）。本字段是只读载体，{@code @Data} 自带的 setter
 * 仅供序列化层（MyBatis-Plus / Jackson）使用，业务侧不应调用；
 * 实际填充 = 由后续 owner 裁 D-00X 时新增 {@code SaiAgentConversationMapper} JOIN 写入。
 * 本 D-002 <b>不代裁</b>、<b>不预造 mapper</b>，仅占位字段以便接口契约先稳定。
 *
 * @author AIP
 */
@Data
public class DeBindingVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** flow_instance_biz_ext.instance_id */
    private Long instanceId;

    /** flow_instance_biz_ext.business_code */
    private String businessCode;

    /** flow_instance_biz_ext.business_id（字符串形式的 sai_agent.id） */
    private String agentId;

    /** sai_agent.name（JOIN 读回） */
    private String agentName;

    /**
     * 派生自 sai_agent_conversation.title（与 create_dt 共同表达「最近一轮会话标题」）——
     * AU-09 只读字段，不在 de/ 服务层写入。
     */
    private String title;
}