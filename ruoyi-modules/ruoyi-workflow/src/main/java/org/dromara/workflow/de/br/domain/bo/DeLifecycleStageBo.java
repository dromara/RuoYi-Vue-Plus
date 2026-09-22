package org.dromara.workflow.de.br.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * BR-04 数字员工生命周期阶段推进请求。
 *
 * <p>落点：{@code sai_agent.status}（与 BR-01 同一列，但语义层 = 生命周期阶段）。
 */
@Data
public class DeLifecycleStageBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** sai_agent.id */
    @NotNull(message = "agentId 不能为空")
    private Long agentId;

    /**
     * 目标阶段：{@code DRAFT / TEST / AVAILABLE / DISABLE / ARCHIVE / RESTORE / REPLACE}。
     * 应用层阶段语义；与 sai_agent.status 的物理枚举有一对一映射（DRAFT→1 草稿占位由 caller 维护，
     * TEST/AVAILABLE→1，D→DISABLE→4，ARCHIVE→3，RESTORE/REPLACE→1）。
     */
    @NotBlank(message = "stage 不能为空")
    private String stage;

    /** 阶段推进原因 */
    private String reason;
}
