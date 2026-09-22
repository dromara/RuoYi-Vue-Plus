package org.dromara.workflow.de.br.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * BR-08 记忆与技能版本差异 / 冲突合并 / 回滚请求。
 *
 * <p>落点：{@code sai_agent_conversation_record.metadata} + {@code sai_skill.version}。
 */
@Data
public class DeSkillVersionDiffBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** sai_agent_conversation_record.id */
    @NotNull(message = "recordId 不能为空")
    private Long recordId;

    /** sai_skill.id */
    @NotNull(message = "skillId 不能为空")
    private Long skillId;

    /** 操作类型 */
    @NotBlank(message = "operation 不能为空")
    private String operation; // DIFF | MERGE | ROLLBACK
}
