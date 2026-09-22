package org.dromara.workflow.de.br.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * BR-09 执行模式请求（自动 / 人工 / 协助）。
 *
 * <p>落点：{@code flow_task.node_type}（TINYINT 0/1/2/3/4）。
 */
@Data
public class DeExecutionModeBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** flow_task.id */
    @NotNull(message = "taskId 不能为空")
    private Long taskId;

    /** 执行模式：{@code AUTO / MANUAL / ASSIST} */
    @NotBlank(message = "mode 不能为空")
    private String mode;
}
