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
     * + 实例创建人（create_by，用户名字符串）定位绑定实例（sai_agent_conversation
     * 不落 instance_id 列，绑定真源 = variable JSON；E2E §五 ctx_ag 断言同源）。
     *
     * ★ 2026-09-22 安全审计 S-02 订正：本方法曾改为 agentId 单键（辩护「订阅校验
     *   已保障属主」被驳——订阅证明有权**用** agent，不证明反查到的实例**属于**你），
     *   在 agent 多订阅者场景构成跨用户写回（活库 agent 7 双订阅者实证）。现按
     *   create_by = 当前登录用户名过滤，错配返回 null → 走 warn 跳过分支 = fail-safe。
     *   正解（对话创建时锚定 instance 关联，须加列）= owner 裁决项（C 组预案 S-02 追记）。
     *
     * ★ 比较规则：agentId 走数值（CAST AS UNSIGNED—— 字符串比较会踩 MySQL 1267
     *   collation 冲突，variable 列 utf8mb4_general_ci vs 连接 utf8mb4_0900_ai_ci，
     *   异常 8 家族）；username 走列-参数等值（参数强制力弱于列 ⇒ 列 collation 胜出，
     *   与底座自身「flow_instance.create_by = 主体」过滤同型，无 1267）。
     *
     * @param agentId  数字员工 id
     * @param username 当前登录用户名（与 flow_instance.create_by 同型的字符串）
     * @return 绑定实例 id（最新一条）；未绑定或非本人发起返回 null
     */
    @Select("SELECT id FROM flow_instance WHERE del_flag = '0' AND JSON_VALID(variable) = 1 "
        + "AND CAST(JSON_EXTRACT(variable, '$.\"de.ctx\".binding.agent_id') AS UNSIGNED) = #{agentId} "
        + "AND create_by = #{username} "
        + "ORDER BY id DESC LIMIT 1")
    Long findInstanceIdByBinding(@Param("agentId") Long agentId, @Param("username") String username);
}
