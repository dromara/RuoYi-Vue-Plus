package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.dto.RoleDTO;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.workflow.common.constant.FlowConstant;
import org.dromara.workflow.domain.bo.CompleteTaskBo;
import org.dromara.workflow.domain.bo.StartProcessBo;
import org.dromara.workflow.domain.bo.TaskBo;
import org.dromara.workflow.domain.vo.TaskVo;
import org.dromara.workflow.service.IActTaskService;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务 服务层实现
 *
 * @author may
 */
@RequiredArgsConstructor
@Service
public class ActTaskServiceImpl implements IActTaskService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;

    /**
     * 启动任务
     *
     * @param startProcessBo 启动流程参数
     */
    @Override
    public Map<String, Object> startWorkFlow(StartProcessBo startProcessBo) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isBlank(startProcessBo.getBusinessKey())) {
            throw new ServiceException("启动工作流时必须包含业务ID");
        }
        // 判断当前业务是否启动过流程
        List<HistoricProcessInstance> instanceList = historyService.createHistoricProcessInstanceQuery()
            .processInstanceBusinessKey(startProcessBo.getBusinessKey()).processInstanceTenantIdLike(TenantHelper.getTenantId()).list();
        TaskQuery taskQuery = taskService.createTaskQuery();
        List<Task> taskResult = taskQuery.processInstanceBusinessKey(startProcessBo.getBusinessKey()).taskTenantId(TenantHelper.getTenantId()).list();
        if (CollUtil.isNotEmpty(instanceList)) {
            map.put("processInstanceId", taskResult.get(0).getProcessInstanceId());
            map.put("taskId", taskResult.get(0).getId());
            return map;
        }
        // 设置启动人
        Authentication.setAuthenticatedUserId(LoginHelper.getUserId().toString());
        // 启动流程实例（提交申请）
        Map<String, Object> variables = startProcessBo.getVariables();
        // 启动跳过表达式
        variables.put("_FLOWABLE_SKIP_EXPRESSION_ENABLED", true);
        ProcessInstance pi;
        if (CollUtil.isNotEmpty(variables)) {
            pi = runtimeService.startProcessInstanceByKeyAndTenantId(startProcessBo.getProcessKey(), startProcessBo.getBusinessKey(), variables, TenantHelper.getTenantId());
        } else {
            pi = runtimeService.startProcessInstanceByKeyAndTenantId(startProcessBo.getProcessKey(), startProcessBo.getBusinessKey(), TenantHelper.getTenantId());
        }
        // 将流程定义名称 作为 流程实例名称
        runtimeService.setProcessInstanceName(pi.getProcessInstanceId(), pi.getProcessDefinitionName());
        // 申请人执行流程
        List<Task> taskList = taskService.createTaskQuery().processInstanceId(pi.getId()).taskTenantId(TenantHelper.getTenantId()).list();
        if (taskList.size() > 1) {
            throw new ServiceException("请检查流程第一个环节是否为申请人！");
        }
        taskService.setAssignee(taskList.get(0).getId(), LoginHelper.getUserId().toString());
        taskService.setVariable(taskList.get(0).getId(), "processInstanceId", pi.getProcessInstanceId());
        map.put("processInstanceId", pi.getProcessInstanceId());
        map.put("taskId", taskList.get(0).getId());
        return map;
    }

    /**
     * 办理任务
     *
     * @param completeTaskBo 办理任务参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeTask(CompleteTaskBo completeTaskBo) {
        try {
            List<RoleDTO> roles = LoginHelper.getLoginUser().getRoles();
            String userId = LoginHelper.getUserId().toString();
            TaskQuery taskQuery = taskService.createTaskQuery();
            taskQuery.taskId(completeTaskBo.getTaskId())
                .taskTenantId(TenantHelper.getTenantId()).taskCandidateOrAssigned(userId);
            if (CollUtil.isNotEmpty(roles)) {
                List<String> groupIds = roles.stream().map(e -> String.valueOf(e.getRoleId())).collect(Collectors.toList());
                taskQuery.taskCandidateGroupIn(groupIds);
            }
            Task task = taskQuery.singleResult();
            if (task == null) {
                throw new ServiceException(FlowConstant.MESSAGE_CURRENT_TASK_IS_NULL);
            }
            //办理任务
            taskService.complete(completeTaskBo.getTaskId(), completeTaskBo.getVariables());
            //办理意见
            taskService.addComment(completeTaskBo.getTaskId(), task.getProcessInstanceId(), StringUtils.isBlank(completeTaskBo.getMessage()) ? "同意" : completeTaskBo.getMessage());
            return true;
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 查询当前用户的待办任务
     *
     * @param taskBo 参数
     */
    @Override
    public TableDataInfo<TaskVo> getTaskWaitByPage(TaskBo taskBo) {
        List<RoleDTO> roles = LoginHelper.getLoginUser().getRoles();
        String userId = LoginHelper.getUserId().toString();
        TaskQuery query = taskService.createTaskQuery();
        query.taskTenantId(TenantHelper.getTenantId()).taskCandidateOrAssigned(userId);
        if (CollUtil.isNotEmpty(roles)) {
            List<String> groupIds = roles.stream().map(e -> String.valueOf(e.getRoleId())).collect(Collectors.toList());
            query.taskCandidateGroupIn(groupIds);
        }
        if (StringUtils.isNotBlank(taskBo.getName())) {
            query.taskNameLike(taskBo.getName());
        }
        List<Task> taskList = query.listPage(taskBo.getPageNum(), taskBo.getPageSize());
        List<TaskVo> list = new ArrayList<>();
        for (Task task : taskList) {
            list.add(BeanUtil.toBean(task, TaskVo.class));
        }
        long count = query.count();
        return new TableDataInfo<>(list, count);
    }

    /**
     * 查询当前用户的已办任务
     *
     * @param taskBo 参数
     */
    @Override
    public TableDataInfo<TaskVo> getTaskFinishByPage(TaskBo taskBo) {
        String userId = LoginHelper.getUserId().toString();
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
            .taskAssignee(userId).taskTenantId(TenantHelper.getTenantId()).finished().orderByHistoricTaskInstanceStartTime().asc();
        if (StringUtils.isNotBlank(taskBo.getName())) {
            query.taskNameLike(taskBo.getName());
        }
        List<HistoricTaskInstance> taskInstanceList = query.listPage(taskBo.getPageNum(), taskBo.getPageSize());
        List<TaskVo> list = new ArrayList<>();
        for (HistoricTaskInstance task : taskInstanceList) {
            list.add(BeanUtil.toBean(task, TaskVo.class));
        }
        long count = query.count();
        return new TableDataInfo<>(list, count);
    }
}
