package org.dromara.workflow.liteflow.complete;

import cn.hutool.core.convert.Convert;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.enums.SkipType;
import org.dromara.warm.flow.core.service.TaskService;
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

    private final TaskService taskService;

    @Override
    public void process() {
        CompleteTaskContext context = getContextBean(CompleteTaskContext.class);
        CompleteTaskBo completeTaskBo = context.getCompleteTaskBo();
        Map<String, Object> variables = completeTaskBo.getVariables();
        FlowParams flowParams = FlowParams.build()
            .handler(completeTaskBo.getHandler())
            .variable(variables)
            .ignore(Convert.toBool(variables.getOrDefault(VAR_IGNORE, false)))
            .ignoreDepute(Convert.toBool(variables.getOrDefault(VAR_IGNORE_DEPUTE, false)))
            .ignoreCooperate(Convert.toBool(variables.getOrDefault(VAR_IGNORE_COOPERATE, false)))
            .skipType(SkipType.PASS.getKey())
            .message(completeTaskBo.getMessage())
            .flowStatus(BusinessStatusEnum.WAITING.getStatus())
            .hisStatus(TaskStatusEnum.PASS.getStatus())
            .hisTaskExt(completeTaskBo.getFileId());

        context.setFlowParams(flowParams);
        context.setAutoPass(Convert.toBool(context.getInstance().getVariableMap().getOrDefault(AUTO_PASS, false)));
        taskService.skip(context.getFlowTask().getId(), flowParams);
    }

}
