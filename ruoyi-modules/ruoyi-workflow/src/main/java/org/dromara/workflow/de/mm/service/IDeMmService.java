package org.dromara.workflow.de.mm.service;

import org.dromara.workflow.de.mm.domain.vo.DeMmConfigVo;
import org.dromara.workflow.de.mm.domain.vo.DeMmConversationDetailVo;
import org.dromara.workflow.de.mm.domain.vo.DeMmMemoryVo;

import java.util.List;

/**
 * 记忆与经验（MM）服务接口 —— 29 号 §五 MM-01~MM-07 取 3 端点切片。
 *
 * <p>落点真源：sai_agent_conversation_record（metadata / content / user_id /
 * conversation_id / role / status）＋ 只读引用 sai_agent.memory_enabled。
 * 底座无记忆分层位 / 分类位 / 保留期位（29 号 MM-01/02/07 改写口径），
 * 本域只做检索 / 回溯 / 配置读，不新造表。
 *
 * @author AIP
 */
public interface IDeMmService {

    /**
     * MM-01 / MM-02：按用户 / Agent / 会话 / 角色 / 关键词检索记忆记录。
     */
    List<DeMmMemoryVo> listMemories(Long userId, Long agentId, String conversationId, String role, String keyword);

    /**
     * MM-03 / MM-04：单会话上下文回溯 —— 全量历史（按 id 升序）+ 摘要
     * （条数 / 角色分布 / 状态分布 / metadata 覆盖 / 首末内容）。
     */
    DeMmConversationDetailVo conversationDetail(String conversationId);

    /**
     * MM-01：Agent 记忆配置读 —— sai_agent.memory_enabled + 记录载荷统计。
     */
    DeMmConfigVo memoryConfig(Long agentId);
}
