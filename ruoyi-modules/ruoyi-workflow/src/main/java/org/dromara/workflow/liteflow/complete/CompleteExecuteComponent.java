package org.dromara.workflow.liteflow.complete;

import cn.hutool.core.convert.Convert;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.enums.TaskStatus;
import org.dromara.warm.flow.core.workflow.command.CompleteCommand;
import org.dromara.warm.flow.core.workflow.context.OperatorContext;
import org.dromara.workflow.common.ConditionalOnEnable;
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
        // 全局监听器用 context.taskStatus 判断「通过/退回」，才决定要不要抄送与发通知；
        // 引擎不会把解析后的状态回填进调用上下文，办理路径不显式声明 PASS 就会静默跳过抄送。
        command.setTaskStatus(TaskStatus.PASS.getKey());
        FlowEngine.workflow().complete(command);
    }

}
