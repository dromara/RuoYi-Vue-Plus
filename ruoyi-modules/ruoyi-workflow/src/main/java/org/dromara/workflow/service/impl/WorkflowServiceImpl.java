package org.dromara.workflow.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.service.WorkflowService;
import org.dromara.workflow.service.IFlwInstanceService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 通用 工作流服务实现
 *
 * @author may
 */
@RequiredArgsConstructor
@Service
public class WorkflowServiceImpl implements WorkflowService {

    private final IFlwInstanceService iFlwInstanceService;

    /**
     * 删除流程实例
     *
     * @param businessKeys 业务id
     * @return 结果
     */
    @Override
    public boolean deleteInstance(List<Long> businessKeys) {
        return iFlwInstanceService.deleteByBusinessIds(businessKeys);
    }

    /**
     * 获取当前流程状态
     *
     * @param taskId 任务id
     */
    @Override
    public String getBusinessStatusByTaskId(String taskId) {
        return "";
    }

    /**
     * 获取当前流程状态
     *
     * @param businessKey 业务id
     */
    @Override
    public String getBusinessStatus(String businessKey) {
        return "";
    }

    /**
     * 设置流程变量(全局变量)
     *
     * @param taskId       任务id
     * @param variableName 变量名称
     * @param value        变量值
     */
    @Override
    public void setVariable(String taskId, String variableName, Object value) {
    }

    /**
     * 设置流程变量(全局变量)
     *
     * @param taskId    任务id
     * @param variables 流程变量
     */
    @Override
    public void setVariables(String taskId, Map<String, Object> variables) {
    }

    /**
     * 设置流程变量(本地变量,非全局变量)
     *
     * @param taskId       任务id
     * @param variableName 变量名称
     * @param value        变量值
     */
    @Override
    public void setVariableLocal(String taskId, String variableName, Object value) {
    }

    /**
     * 设置流程变量(本地变量,非全局变量)
     *
     * @param taskId    任务id
     * @param variables 流程变量
     */
    @Override
    public void setVariablesLocal(String taskId, Map<String, Object> variables) {
    }

    /**
     * 按照业务id查询流程实例id
     *
     * @param businessKey 业务id
     * @return 结果
     */
    @Override
    public String getInstanceIdByBusinessKey(String businessKey) {
        return null;
    }
}
