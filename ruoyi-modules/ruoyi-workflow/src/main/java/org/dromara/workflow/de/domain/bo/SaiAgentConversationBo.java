package org.dromara.workflow.de.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 智能体对话表 sai_agent_conversation 请求参数（BO）。
 *
 * <p>对应 REST 端点 {@code /de/sai-conv}：
 * <ul>
 *   <li>POST /de/sai-conv —— 新增对话（add）</li>
 *   <li>PUT /de/sai-conv —— 修改对话（edit）</li>
 *   <li>DELETE /de/sai-conv/{ids} —— 删除（remove）</li>
 *   <li>GET /de/sai-conv/{id} —— 详情（getById）</li>
 *   <li>GET /de/sai-conv/list —— 分页列表（list）</li>
 *   <li>GET /de/sai-conv/by-agent/{agentId} —— 按 agent_id 查询（listByAgentId）</li>
 *   <li>GET /de/sai-conv/record/{recordId} —— 按 record_id 查询 sai_agent_conversation_record 单条</li>
 * </ul>
 *
 * <p><b>字段清单</b>（DDL 真源 = {@code 04-backend-code/script/sql/ry_ai.sql} 7 列，
 * 与 {@code docs/底座真实表结构.md} 一致）：{@code id · agentId · userId ·
 * conversationId · title · createDt · updateDt}。{@code agentId} 作为分页过滤条件
 * （{@code listByAgentId} 强依赖字段）。
 *
 * @author AIP
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SaiAgentConversationBo extends PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 智能体ID（sai_agent.id） */
    private Long agentId;

    /** 用户ID（sys_user.user_id） */
    private Long userId;

    /** 对话ID（UUID） */
    private String conversationId;

    /** 对话标题（AU-09 派生字段真源） */
    private String title;

    /** 创建时间（过滤用） */
    private Date createDt;

    /** 更新时间（过滤用） */
    private Date updateDt;
}