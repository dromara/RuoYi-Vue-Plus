package org.dromara.workflow.de.br.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * BR-16 业务取消 / 回退 / 补偿入口请求。
 *
 * <p>落点：{@code flow_skip.skip_type}（与 BR-15 同一列）。
 */
@Data
public class DeSkipCompensationBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** flow_instance.id */
    @NotNull(message = "instanceId 不能为空")
    private Long instanceId;

    /** 操作类型 */
    @NotBlank(message = "operation 不能为空")
    private String operation; // CANCEL | ROLLBACK | COMPENSATE

    /** 原因（必填） */
    @NotBlank(message = "reason 不能为空")
    private String reason;
}
