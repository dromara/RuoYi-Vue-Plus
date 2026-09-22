package org.dromara.workflow.de.rs.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * RS-05 审核者决定 BO（接受 / 退回 / 要求补充）。
 *
 * @author AIP
 */
@Data
public class DeRsDecideBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long taskId;

    /** accept / reject / require-more */
    @NotBlank
    private String decision;

    private String comment;
}