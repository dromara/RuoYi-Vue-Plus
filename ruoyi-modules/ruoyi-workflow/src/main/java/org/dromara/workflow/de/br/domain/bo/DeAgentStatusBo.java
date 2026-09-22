package org.dromara.workflow.de.br.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * BR-01 数字员工状态机转移请求。
 *
 * <p>落点：{@code sai_agent.status}（TINYINT 枚举 1=活跃 2=非活跃 3=已废弃 4=已禁用）。
 */
@Data
public class DeAgentStatusBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** sai_agent.id */
    @NotNull(message = "agentId 不能为空")
    private Long agentId;

    /** 当前 status（仅作校验用，避免 stale read） */
    @NotNull(message = "fromStatus 不能为空")
    private Integer fromStatus;

    /** 目标 status */
    @NotNull(message = "toStatus 不能为空")
    private Integer toStatus;

    /** 转移原因（必填，便于审计 / 后续 flow_his_task 引用） */
    private String reason;
}
