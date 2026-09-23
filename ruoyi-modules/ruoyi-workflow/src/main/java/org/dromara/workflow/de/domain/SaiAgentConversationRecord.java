package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 智能体对话消息记录表 sai_agent_conversation_record 实体（DDL 真值 04-backend-code/script/sql/ry_ai.sql:184）。
 *
 * <p>BR-06 / BR-08 「记忆 metadata」的来源表。仅暴露 5 列（id · metadata · status · role · content）。
 */
@Data
@TableName("sai_agent_conversation_record")
public class SaiAgentConversationRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 智能体 ID（外键 sai_agent.id） */
    private Long agentId;

    /** 用户 ID（外键 sys_user.user_id） */
    private Long userId;

    /**
     * 对话ID（列在 DDL 中存在；T-3「实例内」判定经此反查对话锚定。2026-09-22 补映射）
     */
    private String conversationId;

    /** 角色 user/assistant */
    private String role;

    /** 消息内容 */
    private String content;

    /** 消息扩展元数据 JSON（BR-06 de.scope / BR-08 de.pinned_skill_version 的落点） */
    private String metadata;

    /** 状态 1=成功 2=失败 3=进行中 */
    private Integer status;
}
