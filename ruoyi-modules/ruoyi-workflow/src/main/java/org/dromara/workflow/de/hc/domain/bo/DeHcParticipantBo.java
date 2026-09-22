package org.dromara.workflow.de.hc.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * HC-05 参与者变动 BO（加入 / 退出 / 替补 / 转派）。
 *
 * @author AIP
 */
@Data
public class DeHcParticipantBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long taskId;

    @NotNull
    private Long userId;

    /** join / leave / replace / transfer */
    @NotBlank
    private String action;

    /** 替补 / 转派 目标 ID */
    private Long targetUserId;
}