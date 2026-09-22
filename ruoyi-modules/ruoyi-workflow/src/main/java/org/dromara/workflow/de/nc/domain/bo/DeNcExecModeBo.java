package org.dromara.workflow.de.nc.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * NC-06 执行模式 BO（人工 / 协助 / 自动）。
 *
 * @author AIP
 */
@Data
public class DeNcExecModeBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long nodeId;

    /** manual / assist / auto */
    @NotBlank
    private String execMode;
}