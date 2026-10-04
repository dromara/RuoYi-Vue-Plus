package org.dromara.workflow.liteflow.complete;

import cn.hutool.core.convert.Convert;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.workflow.command.CompleteCommand;
import org.dromara.warm.flow.core.workflow.context.OperatorContext;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.common.enums.TaskStatusEnum;
import org.dromara.workflow.domain.bo.CompleteTaskBo;
import org.dromara.workflow.domain.context.CompleteTaskContext;

import java.util.Map;

import static org.dromara.workflow.common.constant.FlowConstant.*;

/**
 * 构建办理参数并执行当前任务跳转。
 *
 * @author may
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@LiteflowComponent("completeExecute")
public class CompleteExecuteComponent extends NodeComponent {

    @Override
    public void process() {
        CompleteTaskContext context = getContextBean(CompleteTaskContext.class);
        CompleteTaskBo completeTaskBo = context.getCompleteTaskBo();
        Map<String, Object> variables = completeTaskBo.getVariables();
        CompleteCommand command = new CompleteCommand();
        OperatorContext operator = new OperatorContext(completeTaskBo.getHandler());
        operator.setIgnore(Convert.toBool(variables.getOrDefault(VAR_IGNORE, false)));
        command.setOperator(operator);
        command.setTaskId(context.getFlowTask().getId());
        command.setVariables(variables);
        command.setMessage(completeTaskBo.getMessage());
        command.setExt(completeTaskBo.getFileId());
        command.setFlowStatus(BusinessStatusEnum.WAITING.getStatus());
        command.setTaskStatus(TaskStatusEnum.PASS.getStatus());
        context.setAutoPass(Convert.toBool(context.getInstance().getVariableMap().getOrDefault(AUTO_PASS, false)));
        FlowEngine.workflow().complete(command);
    }

}
