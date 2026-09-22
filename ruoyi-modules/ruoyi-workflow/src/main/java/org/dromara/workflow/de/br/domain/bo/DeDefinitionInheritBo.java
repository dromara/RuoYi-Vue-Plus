package org.dromara.workflow.de.br.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * BR-17 任务撤回 / 转移 / 复制 / 模板继承请求。
 *
 * <p>落点：{@code flow_definition.model_value}（varchar(40)；物理 CLASSICS / MIMIC）。
 */
@Data
public class DeDefinitionInheritBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 源定义 flow_definition.id */
    @NotNull(message = "sourceDefinitionId 不能为空")
    private Long sourceDefinitionId;

    /** 新定义 flow_definition.id（已由 caller 落库） */
    @NotNull(message = "newDefinitionId 不能为空")
    private Long newDefinitionId;

    /** 新定义的 model_value */
    @NotBlank(message = "newModelValue 不能为空")
    private String newModelValue;
}
