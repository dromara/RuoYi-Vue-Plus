package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 技能包表 sai_skill 实体（DDL 真值在 04-backend-code/script/sql/ry_ai.sql:405）。
 *
 * <p><b>本类来源说明</b>：与 {@link SaiAgent} 同模式 —— 底座 DDL 有 / Java 类无，
 * 本类作为 BR-05 / BR-08 校验期的薄映射，<b>不动底座任何文件</b>。
 * 仅暴露 BR 校验需要的 3 列（id · name · version）。
 */
@Data
@TableName("sai_skill")
public class SaiSkill implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** Skill 名称 */
    private String name;

    /** 版本号（DDL:415 BIGINT DEFAULT 0；BR-05 校验核心列） */
    private Long version;
}
