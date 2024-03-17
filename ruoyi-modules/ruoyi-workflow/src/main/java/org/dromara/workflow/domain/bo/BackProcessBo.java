package org.dromara.workflow.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;


/**
 * 驳回参数请求
 *
 * @author may
 */
@Data
public class BackProcessBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    @NotBlank(message = "任务ID不能为空", groups = AddGroup.class)
    private String taskId;

    /**
     * 消息类型
     */
    private List<String> messageType;

    /**
     * 驳回的节点id(目前未使用，直接驳回到申请人)
     */
    @NotBlank(message = "驳回的节点不能为空", groups = AddGroup.class)
    private String targetActivityId;

    /**
     * 办理意见
     */
    private String message;
}
