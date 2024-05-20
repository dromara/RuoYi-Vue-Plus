package org.dromara.workflow.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;

import java.io.Serial;
import java.io.Serializable;

/**
 * 委派任务请求对象
 *
 * @author may
 */
@Data
public class DelegateBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 委派人id
     */
    @NotBlank(message = "委派人id不能为空", groups = {AddGroup.class})
    private String userId;

    /**
     * 委派人名称
     */
    @NotBlank(message = "委派人名称不能为空", groups = {AddGroup.class})
    private String nickName;

    /**
     * 任务id
     */
    @NotBlank(message = "任务id不能为空", groups = {AddGroup.class})
    private String taskId;
}
