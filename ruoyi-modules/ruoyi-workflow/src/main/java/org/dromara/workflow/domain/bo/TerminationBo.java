package org.dromara.workflow.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;

import java.io.Serial;
import java.io.Serializable;

/**
 * 终止任务请求对象
 *
 * @author may
 */
@Data
public class TerminationBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务id
     */
    @NotBlank(message = "任务id为空", groups = AddGroup.class)
    private String taskId;

    /**
     * 审批意见
     */
    private String comment;
}
