package org.dromara.workflow.de.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.de.domain.SaiAgentConversation;
import org.dromara.workflow.de.domain.SaiAgentConversationRecord;
import org.dromara.workflow.de.domain.bo.SaiAgentConversationBo;
import org.dromara.workflow.de.domain.vo.SaiAgentConversationVo;

import java.util.Collection;
import java.util.List;

/**
 * 智能体对话服务接口（sai_agent_conversation + sai_agent_conversation_record）。
 *
 * <p><b>数据源</b>：主表 = {@link SaiAgentConversation}（{@code sai_agent_conversation} 7 列 DDL），
 * 关联只读表 = {@link SaiAgentConversationRecord}（{@code sai_agent_conversation_record} 7 列 DDL，
 * 真源 = {@code 04-backend-code/script/sql/ry_ai.sql:184}）。
 *
 * <p><b>CRUD 范围</b>：
 * <ul>
 *   <li>CRUD + 分页 = {@link SaiAgentConversation} 主表（{@code SaiAgentConversationMapper}）</li>
 *   <li>按 agent_id 查询 = 主表 filter（{@code listByAgentId}）</li>
 *   <li>按 record_id 查询 = {@link SaiAgentConversationRecord} 单条读（{@code SaiAgentConversationRecordMapper}）</li>
 * </ul>
 *
 * @author AIP
 */
public interface IDeSaiConvService {

    /**
     * 新增对话（INSERT sai_agent_conversation）。
     *
     * @param bo 请求参数
     * @return 新写入的主键 id
     */
    Long add(SaiAgentConversationBo bo);

    /**
     * 修改对话（UPDATE sai_agent_conversation BY id）。
     *
     * @param bo 请求参数（必须含 id）
     * @return true 更新成功
     */
    Boolean edit(SaiAgentConversationBo bo);

    /**
     * 删除对话（DELETE sai_agent_conversation BY id 集合）。
     *
     * @param ids 主键集合
     * @return true 删除成功
     */
    Boolean remove(Collection<Long> ids);

    /**
     * 单条详情（SELECT sai_agent_conversation BY id）。
     *
     * @param id 主键
     * @return 单条 VO；未命中返回 null
     */
    SaiAgentConversationVo getById(Long id);

    /**
     * 分页列表（SELECT sai_agent_conversation 按 BO 过滤条件 + PageQuery）。
     *
     * @param bo 请求参数（分页 + 过滤）
     * @return 分页结果（total + rows）
     */
    PageResult<SaiAgentConversationVo> list(SaiAgentConversationBo bo);

    /**
     * 按 agent_id 查询对话列表（无分页；用于会话历史侧栏）。
     *
     * <p>排序：{@code create_dt DESC}。
     *
     * @param agentId 智能体 ID
     * @return VO 列表
     */
    List<SaiAgentConversationVo> listByAgentId(Long agentId);

    /**
     * 按 record_id 查询 {@code sai_agent_conversation_record} 单条。
     *
     * @param recordId 主键
     * @return 单条 record；未命中返回 null
     */
    SaiAgentConversationRecord getRecordById(Long recordId);
}