package org.dromara.workflow.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.listener.GlobalListener;
import org.dromara.warm.flow.core.listener.ListenerVariable;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.handler.FlowProcessEventHandler;
import org.dromara.workflow.service.IFlwInstanceService;
import org.dromara.workflow.service.IFlwTaskService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局任务办理监听
 *
 * @author may
 */
@ConditionalOnEnable
@Component
@Slf4j
@RequiredArgsConstructor
public class WorkflowGlobalListener implements GlobalListener {

    private final IFlwTaskService taskService;
    private final IFlwInstanceService instanceService;
    private final FlowProcessEventHandler flowProcessEventHandler;

    /**
     * 创建监听器，任务创建时执行
     *
     * @param listenerVariable 监听器变量
     */
    @Override
    public void create(ListenerVariable listenerVariable) {
        Instance instance = listenerVariable.getInstance();
        Definition definition = listenerVariable.getDefinition();
        String businessId = instance.getBusinessId();
        String flowStatus = instance.getFlowStatus();
        Task task = listenerVariable.getTask();
        if (task != null && BusinessStatusEnum.WAITING.getStatus().equals(flowStatus)) {
            // 判断流程状态（发布审批中事件）
            flowProcessEventHandler.processCreateTaskHandler(definition.getFlowCode(), task.getNodeCode(), task.getId(), businessId);
        }
    }

    /**
     * 开始监听器，任务开始办理时执行
     *
     * @param listenerVariable 监听器变量
     */
    @Override
    public void start(ListenerVariable listenerVariable) {
    }

    /**
     * 分派监听器，动态修改代办任务信息
     *
     * @param listenerVariable 监听器变量
     */
    @Override
    public void assignment(ListenerVariable listenerVariable) {
    }

    /**
     * 完成监听器，当前任务完成后执行
     *
     * @param listenerVariable 监听器变量
     */
    @Override
    public void finish(ListenerVariable listenerVariable) {
        Instance instance = listenerVariable.getInstance();
        Definition definition = listenerVariable.getDefinition();
        String businessId = instance.getBusinessId();
        String flowStatus = instance.getFlowStatus();
        Map<String, Object> params = new HashMap<>();
        FlowParams flowParams = listenerVariable.getFlowParams();
        if (ObjectUtil.isNotNull(flowParams)) {
            // 历史任务扩展(通常为附件)
            params.put("hisTaskExt", flowParams.getHisTaskExt());
            // 办理人
            params.put("handler", flowParams.getHandler());
            // 办理意见
            params.put("message", flowParams.getMessage());
        }
        // 判断流程状态（发布：撤销，退回，作废，终止，已完成事件）
        String status = determineFlowStatus(instance, flowStatus);
        if (StringUtils.isNotBlank(status)) {
            flowProcessEventHandler.processHandler(definition.getFlowCode(), businessId, status, params, false);
        }
    }

    /**
     * 根据流程实例和当前流程状态确定最终状态
     *
     * @param instance   流程实例
     * @param flowStatus 流程实例当前状态
     * @return 流程最终状态
     */
    private String determineFlowStatus(Instance instance, String flowStatus) {
        if (StringUtils.isNotBlank(flowStatus) && BusinessStatusEnum.initialState(flowStatus)) {
            log.info("流程实例当前状态: {}", flowStatus);
            return flowStatus;
        } else {
            Long instanceId = instance.getId();
            List<FlowTask> flowTasks = taskService.selectByInstId(instanceId);
            if (CollUtil.isEmpty(flowTasks)) {
                String status = BusinessStatusEnum.FINISH.getStatus();
                // 更新流程状态为已完成
                instanceService.updateStatus(instanceId, status);
                log.info("流程已结束，状态更新为: {}", status);
                return status;
            }
            return null;
        }
    }

}
