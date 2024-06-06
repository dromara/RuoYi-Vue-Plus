package org.dromara.workflow.flowable.handler;

import org.dromara.workflow.common.enums.TaskStatusEnum;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.jobexecutor.TimerEventHandler;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.task.api.Task;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * 办理超时(过期)任务
 *
 * @author may
 */
public class TaskTimeoutJobHandler extends TimerEventHandler implements JobHandler {

    public static final String TYPE = "taskTimeout";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        TaskService taskService = CommandContextUtil.getProcessEngineConfiguration(commandContext)
            .getTaskService();
        Task task = taskService.createTaskQuery().taskId(configuration).singleResult();
        if (task != null) {
            taskService.addComment(task.getId(), task.getProcessInstanceId(), TaskStatusEnum.TIMEOUT.getStatus(), "超时自动审批!");
            taskService.complete(configuration);
        }
    }
}
