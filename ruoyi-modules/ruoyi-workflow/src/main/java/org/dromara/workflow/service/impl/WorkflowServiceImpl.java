package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.dto.CompleteTaskDTO;
import org.dromara.common.core.domain.dto.StartProcessDTO;
import org.dromara.common.core.domain.dto.StartProcessReturnDTO;
import org.dromara.common.core.service.WorkflowService;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.domain.bo.CompleteTaskBo;
import org.dromara.workflow.domain.bo.StartProcessBo;
import org.dromara.workflow.service.IFlwDefinitionService;
import org.dromara.workflow.service.IFlwInstanceService;
import org.dromara.workflow.service.IFlwTaskService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 通用 工作流服务实现
 *
 * @author may
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@Service
public class WorkflowServiceImpl implements WorkflowService {

    private final IFlwInstanceService flwInstanceService;
    private final IFlwDefinitionService flwDefinitionService;
    private final IFlwTaskService flwTaskService;

    /**
     * 删除流程实例
     *
     * @param businessIds 业务id
     * @return 结果
     */
    @Override
    public boolean deleteInstance(List<Long> businessIds) {
        return flwInstanceService.deleteByBusinessIds(businessIds);
    }

    /**
     * 获取当前流程状态
     *
     * @param taskId 任务id
     */
    @Override
    public String getBusinessStatusByTaskId(Long taskId) {
        FlowInstance flowInstance = flwInstanceService.selectByTaskId(taskId);
        return ObjectUtil.isNotNull(flowInstance) ? flowInstance.getFlowStatus() : StringUtils.EMPTY;
    }

    /**
     * 获取当前流程状态
     *
     * @param businessId 业务id
     */
    @Override
    public String getBusinessStatus(String businessId) {
        FlowInstance flowInstance = flwInstanceService.selectInstByBusinessId(businessId);
        return ObjectUtil.isNotNull(flowInstance) ? flowInstance.getFlowStatus() : StringUtils.EMPTY;
    }

    /**
     * 设置流程变量
     *
     * @param instanceId 流程实例id
     * @param variables  流程变量
     */
    @Override
    public void setVariable(Long instanceId, Map<String, Object> variables) {
        flwInstanceService.setVariable(instanceId, variables);
    }

    /**
     * 获取流程变量
     *
     * @param instanceId 流程实例id
     */
    @Override
    public Map<String, Object> instanceVariable(Long instanceId) {
        return flwInstanceService.instanceVariable(instanceId);
    }

    /**
     * 按照业务id查询流程实例id
     *
     * @param businessId 业务id
     * @return 结果
     */
    @Override
    public Long getInstanceIdByBusinessId(String businessId) {
        FlowInstance flowInstance = flwInstanceService.selectInstByBusinessId(businessId);
        return ObjectUtil.isNotNull(flowInstance) ? flowInstance.getId() : null;
    }

    /**
     * 新增租户流程定义
     *
     * @param tenantId 租户id
     */
    @Override
    public void syncDef(String tenantId) {
        flwDefinitionService.syncDef(tenantId);
    }

    /**
     * 启动流程
     *
     * @param startProcess 参数
     */
    @Override
    public StartProcessReturnDTO startWorkFlow(StartProcessDTO startProcess) {
        return flwTaskService.startWorkFlow(BeanUtil.toBean(startProcess, StartProcessBo.class));
    }

    /**
     * 办理任务
     *
     * @param completeTask 参数
     */
    @Override
    public boolean completeTask(CompleteTaskDTO completeTask) {
        return flwTaskService.completeTask(BeanUtil.toBean(completeTask, CompleteTaskBo.class));
    }
}
