package org.dromara.workflow.flowable.config;

import cn.hutool.core.collection.CollUtil;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.workflow.common.enums.TaskStatusEnum;
import org.dromara.workflow.flowable.handler.TaskTimeoutJobHandler;
import org.dromara.workflow.utils.QueryUtils;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.common.engine.api.delegate.event.*;
import org.flowable.common.engine.impl.cfg.TransactionState;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.task.Comment;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.task.api.Task;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;


/**
 * 引擎调度监听
 *
 * @author may
 */
@Component
public class GlobalFlowableListener implements FlowableEventListener {

    @Autowired
    @Lazy
    private TaskService taskService;

    @Autowired
    @Lazy
    private RuntimeService runtimeService;

    @Autowired
    @Lazy
    private RepositoryService repositoryService;

    @Value("${flowable.async-executor-activate}")
    private boolean asyncExecutorActivate;

    @Override
    public void onEvent(FlowableEvent flowableEvent) {
        if (flowableEvent instanceof FlowableEngineEvent flowableEngineEvent) {
            FlowableEngineEventType engineEventType = (FlowableEngineEventType) flowableEvent.getType();
            switch (engineEventType) {
                case JOB_EXECUTION_SUCCESS -> jobExecutionSuccess((FlowableEngineEntityEvent) flowableEngineEvent);
                case TASK_DUEDATE_CHANGED, TASK_CREATED -> {
                    FlowableEntityEvent flowableEntityEvent = (FlowableEntityEvent) flowableEngineEvent;
                    Object entityObject = flowableEntityEvent.getEntity();
                    TaskEntity task = (TaskEntity) entityObject;
                    if (asyncExecutorActivate && task.getDueDate() != null && task.getDueDate().after(new Date())) {
                        //删除之前已经存在的定时任务
                        TimerJobService timerJobService = CommandContextUtil.getTimerJobService();
                        List<TimerJobEntity> timerJobEntityList = timerJobService.findTimerJobsByProcessInstanceId(task.getProcessInstanceId());
                        if (!CollUtil.isEmpty(timerJobEntityList)) {
                            for (TimerJobEntity timerJobEntity : timerJobEntityList) {
                                String taskId = timerJobEntity.getJobHandlerConfiguration();
                                if (task.getId().equals(taskId)) {
                                    timerJobService.deleteTimerJob(timerJobEntity);
                                }
                            }
                        }
                        //创建job对象
                        TimerJobEntity timer = timerJobService.createTimerJob();
                        timer.setTenantId(TenantHelper.getTenantId());
                        //设置job类型
                        timer.setJobType(JobEntity.JOB_TYPE_TIMER);
                        timer.setJobHandlerType(TaskTimeoutJobHandler.TYPE);
                        timer.setDuedate(task.getDueDate());
                        timer.setProcessInstanceId(task.getProcessInstanceId());
                        //设置任务id
                        timer.setJobHandlerConfiguration(task.getId());
                        //保存并触发事件
                        timerJobService.scheduleTimerJob(timer);
                    }
                }
            }
        }
    }

    @Override
    public boolean isFailOnException() {
        return true;
    }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        return false;
    }

    @Override
    public String getOnTransaction() {
        return TransactionState.COMMITTED.name();
    }

    /**
     * 处理边界定时事件自动审批记录
     *
     * @param event 事件
     */
    protected void jobExecutionSuccess(FlowableEngineEntityEvent event) {
        if (event != null && StringUtils.isNotBlank(event.getExecutionId())) {
            Execution execution = runtimeService.createExecutionQuery().executionId(event.getExecutionId()).singleResult();
            if (execution != null) {
                BpmnModel bpmnModel = repositoryService.getBpmnModel(event.getProcessDefinitionId());
                FlowElement flowElement = bpmnModel.getFlowElement(execution.getActivityId());
                if (flowElement instanceof BoundaryEvent) {
                    String attachedToRefId = ((BoundaryEvent) flowElement).getAttachedToRefId();
                    List<Execution> list = runtimeService.createExecutionQuery().activityId(attachedToRefId).list();
                    for (Execution ex : list) {
                        Task task = QueryUtils.taskQuery().executionId(ex.getId()).singleResult();
                        if (task != null) {
                            List<Comment> taskComments = taskService.getTaskComments(task.getId());
                            if (CollUtil.isEmpty(taskComments)) {
                                taskService.addComment(task.getId(), task.getProcessInstanceId(), TaskStatusEnum.PASS.getStatus(), "超时自动审批!");
                            }
                        }
                    }
                }
            }
        }
    }
}
