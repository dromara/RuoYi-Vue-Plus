package org.dromara.de.conversation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.de.conversation.domain.DeConversation;

/**
 * 数字员工对话 Mapper
 *
 * @author AIP
 * @since 2026-09-22
 */
public interface DeConversationMapper extends BaseMapper<DeConversation> {

    /**
     * AC-11 实例反查：按 {@code flow_instance.variable} 的 de.ctx.binding.agent_id
     * 定位当前绑定实例（sai_agent_conversation 不落 instance_id 列，绑定真源 = variable JSON；
     * E2E §五 ctx_ag 断言同源）。
     * ★ 不按 create_by 过滤（2026-09-22 实测订正）：flow_instance.create_by 存的是
     *   用户名字符串（夹具与实跑均为 'admin'），不是数字 userId —— 集成步骤文档
     *   步骤 2 的 findInstanceIdByAgentAndUser 草图按 id 过滤在真实数据下恒 0 行。
     *   对话属主已在创建时过 sai_user_agent 订阅校验（Fix #2），agent 维度单键足够；
     *   AU-10 唯一绑定（同实例至多一行）也是 agent 维度。
     * ★ 比较一律走数值（CAST AS UNSIGNED）—— 字符串比较会踩 MySQL 1267 collation
     *   冲突（variable 列 utf8mb4_general_ci vs 连接 utf8mb4_0900_ai_ci，异常 8 家族；
     *   2026-09-22 E2E 实测 SQLState HY000 / 1267）。
     *
     * @param agentId 数字员工 id
     * @return 绑定实例 id（最新一条）；未绑定返回 null
     */
    @Select("SELECT id FROM flow_instance WHERE del_flag = '0' AND JSON_VALID(variable) = 1 "
        + "AND CAST(JSON_EXTRACT(variable, '$.\"de.ctx\".binding.agent_id') AS UNSIGNED) = #{agentId} "
        + "ORDER BY id DESC LIMIT 1")
    Long findInstanceIdByBinding(@Param("agentId") Long agentId);
}
