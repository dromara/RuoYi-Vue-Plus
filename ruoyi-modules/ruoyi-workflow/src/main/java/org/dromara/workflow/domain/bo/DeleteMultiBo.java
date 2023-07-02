package org.dromara.workflow.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 减签参数请求
 *
 * @author may
 */
@Data
public class DeleteMultiBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    @NotBlank(message = "任务ID不能为空", groups = AddGroup.class)
    private String taskId;

    /**
     * 减签人员
     */
    @NotEmpty(message = "减签人员不能为空", groups = AddGroup.class)
    private List<String> taskIds;

    /**
     * 执行id
     */
    @NotEmpty(message = "执行id不能为空", groups = AddGroup.class)
    private List<String> executionIds;

    /**
     * 人员id
     */
    @NotEmpty(message = "减签人员id不能为空", groups = AddGroup.class)
    private List<Long> assigneeIds;

    /**
     * 人员名称
     */
    @NotEmpty(message = "减签人员不能为空", groups = AddGroup.class)
    private List<String> assigneeNames;
}
