package org.dromara.workflow.flowable.cmd;

import org.dromara.common.core.exception.ServiceException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.task.service.HistoricTaskService;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;

import java.util.Date;
import java.util.List;


/**
 * 修改流程历史
 *
 * @author may
 */
public class UpdateHiTaskInstCmd implements Command<Boolean> {

    private final List<String> taskIds;

    private final String processDefinitionId;

    private final String processInstanceId;

    public UpdateHiTaskInstCmd(List<String> taskIds, String processDefinitionId, String processInstanceId) {
        this.taskIds = taskIds;
        this.processDefinitionId = processDefinitionId;
        this.processInstanceId = processInstanceId;
    }

    @Override
    public Boolean execute(CommandContext commandContext) {
        try {
            HistoricTaskService historicTaskService = CommandContextUtil.getHistoricTaskService();
            for (String taskId : taskIds) {
                HistoricTaskInstanceEntity historicTask = historicTaskService.getHistoricTask(taskId);
                if (historicTask != null) {
                    historicTask.setProcessDefinitionId(processDefinitionId);
                    historicTask.setProcessInstanceId(processInstanceId);
                    historicTask.setCreateTime(new Date());
                    CommandContextUtil.getHistoricTaskService().updateHistoricTask(historicTask, true);
                }
            }
            return true;
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }
}
