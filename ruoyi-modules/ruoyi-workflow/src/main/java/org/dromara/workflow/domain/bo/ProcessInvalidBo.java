package org.dromara.workflow.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程实例作废请求对象
 *
 * @author may
 */
@Data
public class ProcessInvalidBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程实例id
     */
    @NotBlank(message = "流程实例id不能为空", groups = {AddGroup.class})
    private String processInstanceId;

    /**
     * 作废原因
     */
    private String deleteReason;
}
