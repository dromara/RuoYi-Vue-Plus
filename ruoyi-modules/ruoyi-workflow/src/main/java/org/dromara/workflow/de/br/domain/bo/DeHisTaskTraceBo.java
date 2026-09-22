package org.dromara.workflow.de.br.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * BR-18 业务取消 / 补偿可追溯结果请求。
 *
 * <p>落点：{@code flow_skip.skip_type} + {@code flow_his_task.message}（varchar(500)）。
 */
@Data
public class DeHisTaskTraceBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** flow_instance.id */
    @NotNull(message = "instanceId 不能为空")
    private Long instanceId;

    /** 操作类型 */
    @NotBlank(message = "operation 不能为空")
    private String operation; // CANCEL | COMPENSATE

    /** flow_skip.skip_type（必填） */
    @NotBlank(message = "skipType 不能为空")
    private String skipType; // PASS | REJECT

    /** flow_his_task.message（必填，≤ 500 字符，必须以 {@code [CANCEL]} 或 {@code [COMPENSATE]} 开头） */
    @NotBlank(message = "message 不能为空")
    private String message;
}
