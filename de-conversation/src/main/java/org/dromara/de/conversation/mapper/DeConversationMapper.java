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
     * AC-11 实例反查（**回退路径**，S-02 正解后仅历史无锚定会话使用）：
     * flow_instance_biz_ext 的 agent_id 索引等值查询（D-009/AU-10）。
     * ★ 对照旧版（JSON_EXTRACT 全表扫 + 1267 collation 规避 + ORDER BY id DESC 漂移）：
     *   数值列 + 数值参数 = 按构造无 collation 问题；idx_agent 索引 seek；语义不变（最新绑定）。
     * ★ create_by 为 bigint userId（biz_ext 列型实测）⇒ 属主过滤用 userId（S-02 fail-safe 保留）。
     *
     * @param agentId 数字员工 id
     * @param userId  当前登录用户 id（biz_ext.create_by 同型）
     * @return 绑定实例 id（最新一条）；未绑定或非本人创建返回 null
     */
    @Select("SELECT instance_id FROM flow_instance_biz_ext "
        + "WHERE agent_id = #{agentId} AND business_code = 'digital_employee.binding' "
        + "AND del_flag = '0' AND create_by = #{userId} "
        + "ORDER BY instance_id DESC LIMIT 1")
    Long findInstanceIdByBinding(@Param("agentId") Long agentId, @Param("userId") Long userId);
}
