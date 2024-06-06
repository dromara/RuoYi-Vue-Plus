package org.dromara.common.core.service;

import java.util.List;
import java.util.Map;

/**
 * 通用 工作流服务
 *
 * @author may
 */
public interface WorkflowService {

    /**
     * 运行中的实例 删除程实例，删除历史记录，删除业务与流程关联信息
     *
     * @param businessKeys 业务id
     * @return 结果
     */
    boolean deleteRunAndHisInstance(List<String> businessKeys);

    /**
     * 获取当前流程状态
     *
     * @param taskId 任务id
     */
    String getBusinessStatusByTaskId(String taskId);

    /**
     * 获取当前流程状态
     *
     * @param businessKey 业务id
     */
    String getBusinessStatus(String businessKey);

    /**
     * 设置流程变量(全局变量)
     *
     * @param taskId       任务id
     * @param variableName 变量名称
     * @param value        变量值
     */
    void setVariable(String taskId, String variableName, Object value);

    /**
     * 设置流程变量(全局变量)
     *
     * @param taskId    任务id
     * @param variables 流程变量
     */
    void setVariables(String taskId, Map<String, Object> variables);

    /**
     * 设置流程变量(本地变量,非全局变量)
     *
     * @param taskId       任务id
     * @param variableName 变量名称
     * @param value        变量值
     */
    void setVariableLocal(String taskId, String variableName, Object value);

    /**
     * 设置流程变量(本地变量,非全局变量)
     *
     * @param taskId    任务id
     * @param variables 流程变量
     */
    void setVariablesLocal(String taskId, Map<String, Object> variables);

    /**
     * 按照业务id查询流程实例id
     *
     * @param businessKey 业务id
     * @return 结果
     */
    String getInstanceIdByBusinessKey(String businessKey);
}
