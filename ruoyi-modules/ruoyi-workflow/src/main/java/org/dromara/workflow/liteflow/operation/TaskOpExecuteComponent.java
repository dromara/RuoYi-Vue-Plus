package org.dromara.workflow.liteflow.operation;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.workflow.command.AddSignerCommand;
import org.dromara.warm.flow.core.workflow.command.DelegateCommand;
import org.dromara.warm.flow.core.workflow.command.RemoveSignerCommand;
import org.dromara.warm.flow.core.workflow.command.TransferCommand;
import org.dromara.warm.flow.core.workflow.context.OperatorContext;
import org.dromara.workflow.common.ConditionalOnEnable;
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

    @Override
    public void process() {
        TaskOperationContext context = getContextBean(TaskOperationContext.class);
        Long taskId = context.getTaskOperationBo().getTaskId();
        boolean result = false;
        OperatorContext operator = context.getOperator();
        String message = context.getMessage();
        switch (context.getOperation()) {
            case DELEGATE_TASK -> {
                DelegateCommand command = new DelegateCommand();
                command.setOperator(operator);
                command.setTaskId(taskId);
                command.setTargetHandler(context.getTaskOperationBo().getUserId());
                command.setMessage(message);
                result = FlowEngine.workflow().delegate(command).isSuccess();
            }
            case TRANSFER_TASK -> {
                TransferCommand command = new TransferCommand();
                command.setOperator(operator);
                command.setTaskId(taskId);
                command.setTargetHandler(context.getTaskOperationBo().getUserId());
                command.setMessage(message);
                result = FlowEngine.workflow().transfer(command).isSuccess();
            }
            case ADD_SIGNATURE -> {
                AddSignerCommand command = new AddSignerCommand();
                command.setOperator(operator);
                command.setTaskId(taskId);
                command.setTargetHandlers(context.getTaskOperationBo().getUserIds());
                command.setMessage(message);
                result = FlowEngine.workflow().addSigner(command).isSuccess();
            }
            case REDUCTION_SIGNATURE -> {
                RemoveSignerCommand command = new RemoveSignerCommand();
                command.setOperator(operator);
                command.setTaskId(taskId);
                command.setTargetHandlers(context.getTaskOperationBo().getUserIds());
                command.setMessage(message);
                result = FlowEngine.workflow().removeSigner(command).isSuccess();
            }
        }
        context.setResult(result);
    }

}
