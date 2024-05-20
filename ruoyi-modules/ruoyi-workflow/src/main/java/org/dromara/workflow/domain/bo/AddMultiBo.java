package org.dromara.workflow.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 加签参数请求
 *
 * @author may
 */
@Data
public class AddMultiBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    @NotBlank(message = "任务ID不能为空", groups = AddGroup.class)
    private String taskId;

    /**
     * 加签人员id
     */
    @NotEmpty(message = "加签人员不能为空", groups = AddGroup.class)
    private List<Long> assignees;

    /**
     * 加签人员名称
     */
    @NotEmpty(message = "加签人员不能为空", groups = AddGroup.class)
    private List<String> assigneeNames;
}
