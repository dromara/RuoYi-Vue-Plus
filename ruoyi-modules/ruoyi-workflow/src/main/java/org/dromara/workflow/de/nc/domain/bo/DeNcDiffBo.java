package org.dromara.workflow.de.nc.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * NC-08 更换执行者 / 资源时显示差异 BO。
 *
 * @author AIP
 */
@Data
public class DeNcDiffBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long nodeId;

    /** 候选新执行者 userId */
    @NotNull
    private Long newAssigneeId;

    /** 候选资源（如 agentId） */
    private Long newResourceId;
}