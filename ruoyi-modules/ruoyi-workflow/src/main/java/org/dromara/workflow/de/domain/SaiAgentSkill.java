package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 智能体与 Skill 关联表 sai_agent_skill 实体（DDL 真值 04-backend-code/script/sql/ry_ai.sql:444）。
 *
 * <p>BR-05 「技能更新影响 Agent 依赖」的来源表。仅暴露 3 列。
 */
@Data
@TableName("sai_agent_skill")
public class SaiAgentSkill implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 智能体 ID（外键 sai_agent.id） */
    private Long agentId;

    /** Skill ID（外键 sai_skill.id） */
    private Long skillId;
}
