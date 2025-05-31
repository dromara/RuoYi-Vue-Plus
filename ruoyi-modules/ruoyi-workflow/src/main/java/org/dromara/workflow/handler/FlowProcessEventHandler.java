package org.dromara.workflow.handler;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.event.ProcessTaskEvent;
import org.dromara.common.core.domain.event.ProcessDeleteEvent;
import org.dromara.common.core.domain.event.ProcessEvent;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 流程监听服务
 *
 * @author may
 * @date 2024-06-02
 */
@ConditionalOnEnable
@Slf4j
@Component
public class FlowProcessEventHandler {

    /**
     * 总体流程监听(例如: 草稿，撤销，退回，作废，终止，已完成，单任务完成等)
     *
     * @param flowCode   流程定义编码
     * @param instance   实例数据
     * @param status     流程状态
     * @param params     办理参数
     * @param submit     当为true时为申请人节点办理
     */
    public void processHandler(String flowCode, Instance instance, String status, Map<String, Object> params, boolean submit) {
        String tenantId = TenantHelper.getTenantId();
        log.info("【流程事件发布】租户ID: {}, 流程编码: {}, 业务ID: {}, 流程状态: {}, 节点类型: {}, 节点编码: {}, 节点名称: {}, 是否申请人节点: {}, 参数: {}",
            tenantId, flowCode, instance.getBusinessId(), status, instance.getNodeType(), instance.getNodeCode(), instance.getNodeName(), submit, params);
        ProcessEvent processEvent = new ProcessEvent();
        processEvent.setTenantId(tenantId);
        processEvent.setFlowCode(flowCode);
        processEvent.setBusinessId(instance.getBusinessId());
        processEvent.setNodeType(instance.getNodeType());
        processEvent.setNodeCode(instance.getNodeCode());
        processEvent.setNodeName(instance.getNodeName());
        processEvent.setStatus(status);
        processEvent.setParams(params);
        processEvent.setSubmit(submit);
        SpringUtils.context().publishEvent(processEvent);
    }

    /**
     * 执行创建任务监听
     *
     * @param flowCode   流程定义编码
     * @param instance   实例数据
     * @param taskId     任务id
     */
    public void processTaskHandler(String flowCode, Instance instance, Long taskId) {
        String tenantId = TenantHelper.getTenantId();
        log.info("【流程任务事件发布】租户ID: {}, 流程编码: {}, 业务ID: {}, 节点类型: {}, 节点编码: {}, 节点名称: {}, 任务ID: {}",
            tenantId, flowCode, instance.getBusinessId(), instance.getNodeType(), instance.getNodeCode(), instance.getNodeName(), taskId);
        ProcessTaskEvent processTaskEvent = new ProcessTaskEvent();
        processTaskEvent.setTenantId(tenantId);
        processTaskEvent.setFlowCode(flowCode);
        processTaskEvent.setBusinessId(instance.getBusinessId());
        processTaskEvent.setNodeType(instance.getNodeType());
        processTaskEvent.setNodeCode(instance.getNodeCode());
        processTaskEvent.setNodeName(instance.getNodeName());
        processTaskEvent.setTaskId(taskId);
        processTaskEvent.setStatus(instance.getFlowStatus());
        SpringUtils.context().publishEvent(processTaskEvent);
    }

    /**
     * 删除流程监听
     *
     * @param flowCode    流程定义编码
     * @param businessId  业务ID
     */
    public void processDeleteHandler(String flowCode, String businessId) {
        String tenantId = TenantHelper.getTenantId();
        log.info("【流程删除事件发布】租户ID: {}, 流程编码: {}, 业务ID: {}", tenantId, flowCode, businessId);
        ProcessDeleteEvent processDeleteEvent = new ProcessDeleteEvent();
        processDeleteEvent.setTenantId(tenantId);
        processDeleteEvent.setFlowCode(flowCode);
        processDeleteEvent.setBusinessId(businessId);
        SpringUtils.context().publishEvent(processDeleteEvent);
    }

}
