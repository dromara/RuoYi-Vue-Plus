package org.dromara.workflow.de.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.workflow.de.domain.SaiAgentConversation;

/**
 * 智能体对话表 sai_agent_conversation 的 Mapper（MyBatis-Plus BaseMapperPlus）。
 *
 * <p>本类与 {@link SaiAgentConversation.java} 同源，均在 P0 期内新建（底座无对应 Java 类）。
 * 后续若底座引入实体，本类应替换为底座版的 mapper。
 *
 * <p><b>AU-09 应用层派生（D-002 已登记）</b>：本 mapper <b>只读</b> —— 由
 * {@code DeBindingServiceImpl.getBinding(Long)} 在读路径调用，按
 * {@code agent_id = #{agentId}} + {@code create_dt DESC LIMIT 1}
 * 取最近一条 conversation 的 {@code title} 回填到 {@code DeBindingVo.title}。
 *
 * <p>本 mapper <b>不写</b> {@code sai_agent_conversation} 表（标题生成属底座 AI 链路范畴，
 * 不在本仓应用层范围）；不补 L1 数据权限注解（AU-09 派生位是只读、无须按 create_by 收口）。
 *
 * @author AIP
 */
public interface SaiAgentConversationMapper extends BaseMapperPlus<SaiAgentConversation, SaiAgentConversation> {
}