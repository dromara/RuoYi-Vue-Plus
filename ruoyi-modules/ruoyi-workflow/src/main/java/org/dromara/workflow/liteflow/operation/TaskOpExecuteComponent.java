package org.dromara.workflow.liteflow.operation;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.common.enums.TaskStatusEnum;
import org.dromara.workflow.domain.context.TaskOperationContext;

/**
 * 执行委派、转办、加签或减签。
 *
 * @author may
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@LiteflowComponent("taskOpExecute")
public class TaskOpExecuteComponent extends NodeComponent {

    private final TaskService taskService;

    @Override
    public void process() {
        TaskOperationContext context = getRequestData();
        Long taskId = context.getTaskOperationBo().getTaskId();
        boolean result = false;
        switch (context.getOperation()) {
            case DELEGATE_TASK -> {
                context.getFlowParams().hisStatus(TaskStatusEnum.DEPUTE.getStatus());
                result = taskService.depute(taskId, context.getFlowParams());
            }
            case TRANSFER_TASK -> {
                context.getFlowParams().hisStatus(TaskStatusEnum.TRANSFER.getStatus());
                result = taskService.transfer(taskId, context.getFlowParams());
            }
            case ADD_SIGNATURE -> {
                context.getFlowParams().hisStatus(TaskStatusEnum.SIGN.getStatus());
                result = taskService.addSignature(taskId, context.getFlowParams());
            }
            case REDUCTION_SIGNATURE -> {
                context.getFlowParams().hisStatus(TaskStatusEnum.SIGN_OFF.getStatus());
                result = taskService.reductionSignature(taskId, context.getFlowParams());
            }
        }
        context.setResult(result);
    }

}
