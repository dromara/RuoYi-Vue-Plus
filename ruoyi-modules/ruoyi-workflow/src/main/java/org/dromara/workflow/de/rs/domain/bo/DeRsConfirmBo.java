package org.dromara.workflow.de.rs.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * RS-07 接收确认 / 权限不足 / 业务取消 / 回退 / 补偿 BO。
 *
 * @author AIP
 */
@Data
public class DeRsConfirmBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long taskId;

    /** confirm / permission-denied / cancel / rollback / compensate */
    @NotBlank
    private String action;

    private String reason;
}