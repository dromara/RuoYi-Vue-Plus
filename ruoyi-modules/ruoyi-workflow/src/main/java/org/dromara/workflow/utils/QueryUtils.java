package org.dromara.workflow.utils;

import cn.hutool.core.bean.BeanUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.workflow.domain.vo.TaskVo;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.DeploymentQuery;
import org.flowable.engine.repository.ModelQuery;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 查询工具
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QueryUtils {

    private static final ProcessEngine PROCESS_ENGINE = SpringUtils.getBean(ProcessEngine.class);

    public static ModelQuery modelQuery() {
        ModelQuery query = PROCESS_ENGINE.getRepositoryService().createModelQuery();
        if (TenantHelper.isEnable()) {
            query.modelTenantId(TenantHelper.getTenantId());
        }
        return query;
    }

    public static ProcessDefinitionQuery definitionQuery() {
        ProcessDefinitionQuery query = PROCESS_ENGINE.getRepositoryService().createProcessDefinitionQuery();
        if (TenantHelper.isEnable()) {
            query.processDefinitionTenantId(TenantHelper.getTenantId());
        }
        return query;
    }

    public static DeploymentQuery deploymentQuery() {
        DeploymentQuery query = PROCESS_ENGINE.getRepositoryService().createDeploymentQuery();
        if (TenantHelper.isEnable()) {
            query.deploymentTenantId(TenantHelper.getTenantId());
        }
        return query;
    }

    public static DeploymentQuery deploymentQuery(String deploymentId) {
        return deploymentQuery().deploymentId(deploymentId);
    }

    public static DeploymentQuery deploymentQuery(List<String> deploymentIds) {
        return deploymentQuery().deploymentIds(deploymentIds);
    }

    public static HistoricTaskInstanceQuery hisTaskInstanceQuery() {
        HistoricTaskInstanceQuery query = PROCESS_ENGINE.getHistoryService().createHistoricTaskInstanceQuery();
        if (TenantHelper.isEnable()) {
            query.taskTenantId(TenantHelper.getTenantId());
        }
        return query;
    }

    public static HistoricTaskInstanceQuery hisTaskInstanceQuery(String processInstanceId) {
        return hisTaskInstanceQuery().processInstanceId(processInstanceId);
    }

    public static HistoricTaskInstanceQuery hisTaskBusinessKeyQuery(String businessKey) {
        return hisTaskInstanceQuery().processInstanceBusinessKey(businessKey);
    }

    public static ProcessInstanceQuery instanceQuery() {
        ProcessInstanceQuery query = PROCESS_ENGINE.getRuntimeService().createProcessInstanceQuery();
        if (TenantHelper.isEnable()) {
            query.processInstanceTenantId(TenantHelper.getTenantId());
        }
        return query;
    }

    public static ProcessInstanceQuery instanceQuery(String processInstanceId) {
        return instanceQuery().processInstanceId(processInstanceId);
    }

    public static ProcessInstanceQuery businessKeyQuery(String businessKey) {
        return instanceQuery().processInstanceBusinessKey(businessKey);
    }

    public static ProcessInstanceQuery instanceQuery(Set<String> processInstanceIds) {
        return instanceQuery().processInstanceIds(processInstanceIds);
    }

    public static HistoricProcessInstanceQuery hisInstanceQuery() {
        HistoricProcessInstanceQuery query = PROCESS_ENGINE.getHistoryService().createHistoricProcessInstanceQuery();
        if (TenantHelper.isEnable()) {
            query.processInstanceTenantId(TenantHelper.getTenantId());
        }
        return query;
    }

    public static HistoricProcessInstanceQuery hisInstanceQuery(String processInstanceId) {
        return hisInstanceQuery().processInstanceId(processInstanceId);
    }

    public static HistoricProcessInstanceQuery hisBusinessKeyQuery(String businessKey) {
        return hisInstanceQuery().processInstanceBusinessKey(businessKey);
    }

    public static HistoricProcessInstanceQuery hisInstanceQuery(Set<String> processInstanceIds) {
        return hisInstanceQuery().processInstanceIds(processInstanceIds);
    }

    public static HistoricActivityInstanceQuery hisActivityInstanceQuery() {
        HistoricActivityInstanceQuery query = PROCESS_ENGINE.getHistoryService().createHistoricActivityInstanceQuery();
        if (TenantHelper.isEnable()) {
            query.activityTenantId(TenantHelper.getTenantId());
        }
        return query;
    }

    public static HistoricActivityInstanceQuery hisActivityInstanceQuery(String processInstanceId) {
        return hisActivityInstanceQuery().processInstanceId(processInstanceId);
    }

    public static TaskQuery taskQuery() {
        TaskQuery query = PROCESS_ENGINE.getTaskService().createTaskQuery();
        if (TenantHelper.isEnable()) {
            query.taskTenantId(TenantHelper.getTenantId());
        }
        return query;
    }

    public static TaskQuery taskQuery(String processInstanceId) {
        return taskQuery().processInstanceId(processInstanceId);
    }

    public static TaskQuery taskQuery(Collection<String> processInstanceIds) {
        return taskQuery().processInstanceIdIn(processInstanceIds);
    }

    /**
     * 按照任务id查询当前任务
     *
     * @param taskId 任务id
     */
    public static TaskVo getTask(String taskId) {
        Task task = PROCESS_ENGINE.getTaskService().createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return null;
        }
        ProcessInstance processInstance = QueryUtils.instanceQuery(task.getProcessInstanceId()).singleResult();
        TaskVo taskVo = BeanUtil.toBean(task, TaskVo.class);
        taskVo.setBusinessKey(processInstance.getBusinessKey());
        taskVo.setMultiInstance(WorkflowUtils.isMultiInstance(task.getProcessDefinitionId(), task.getTaskDefinitionKey()) != null);
        String businessStatus = WorkflowUtils.getBusinessStatus(taskVo.getBusinessKey());
        taskVo.setBusinessStatus(businessStatus);
        return taskVo;
    }
}
