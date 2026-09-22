package org.dromara.workflow.de.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.workflow.de.domain.SaiUserAgent;

/**
 * 用户-智能体订阅表 sai_user_agent 的 Mapper（MyBatis-Plus BaseMapperPlus）。
 *
 * <p>N-05 订阅门禁 = 查本表的 (user_id, agent_id) 是否存在。
 * 底座已有唯一键 uk_user_agent（27 号 §2.8），跨表约束由本服务在应用层兜底。
 *
 * @author AIP
 */
public interface SaiUserAgentMapper extends BaseMapperPlus<SaiUserAgent, SaiUserAgent> {
}