package org.dromara.workflow.de.br.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * BR-05 技能更新 / 回退 / 停用请求。
 *
 * <p>落点：{@code sai_skill.version}（BIGINT）+ {@code sai_agent_skill}（关联表）。
 */
@Data
public class DeSkillUpdateBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** sai_skill.id */
    @NotNull(message = "skillId 不能为空")
    private Long skillId;

    /** 目标 version（UPDATE 必须 > 当前；ROLLBACK 必须 < 当前） */
    @NotNull(message = "targetVersion 不能为空")
    private Long targetVersion;

    /** 操作类型 */
    @NotBlank(message = "operation 不能为空")
    private String operation; // UPDATE | ROLLBACK | DISABLE
}
