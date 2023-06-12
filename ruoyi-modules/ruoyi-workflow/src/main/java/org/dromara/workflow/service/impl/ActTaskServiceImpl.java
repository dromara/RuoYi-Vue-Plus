package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.dto.RoleDTO;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.workflow.common.constant.FlowConstant;
import org.dromara.workflow.domain.bo.*;
import org.dromara.workflow.domain.vo.TaskVo;
import org.dromara.workflow.service.IActTaskService;
import org.dromara.workflow.utils.WorkflowUtils;
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
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Transactional(rollbackFor = Exception.class)
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
        Authentication.setAuthenticatedUserId(String.valueOf(LoginHelper.getUserId()));
        // 启动流程实例（提交申请）
        Map<String, Object> variables = startProcessBo.getVariables();
        // 设置启动人
        variables.put(FlowConstant.INITIATOR, String.valueOf(LoginHelper.getUserId()));
        // 启动跳过表达式
        variables.put("_FLOWABLE_SKIP_EXPRESSION_ENABLED", true);
        ProcessInstance pi = runtimeService.startProcessInstanceByKeyAndTenantId(startProcessBo.getProcessKey(), startProcessBo.getBusinessKey(), variables, TenantHelper.getTenantId());
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
            String userId = String.valueOf(LoginHelper.getUserId());
            TaskQuery taskQuery = taskService.createTaskQuery();
            taskQuery.taskId(completeTaskBo.getTaskId())
                .taskTenantId(TenantHelper.getTenantId()).taskCandidateOrAssigned(userId);
            if (CollUtil.isNotEmpty(roles)) {
                List<String> groupIds = StreamUtils.toList(roles, e -> String.valueOf(e.getRoleId()));
                taskQuery.taskCandidateGroupIn(groupIds);
            }
            Task task = taskQuery.singleResult();
            if (task == null) {
                throw new ServiceException(FlowConstant.MESSAGE_CURRENT_TASK_IS_NULL);
            }
            //办理委托任务
            if (ObjectUtil.isNotEmpty(task.getDelegationState()) && FlowConstant.PENDING.equals(task.getDelegationState().name())) {
                taskService.resolveTask(completeTaskBo.getTaskId());
                TaskEntity newTask = WorkflowUtils.createNewTask(task);
                taskService.addComment(newTask.getId(), task.getProcessInstanceId(), completeTaskBo.getMessage());
                taskService.complete(newTask.getId());
                return true;
            }
            //办理意见
            taskService.addComment(completeTaskBo.getTaskId(), task.getProcessInstanceId(), StringUtils.isBlank(completeTaskBo.getMessage()) ? "同意" : completeTaskBo.getMessage());
            //办理任务
            taskService.complete(completeTaskBo.getTaskId(), completeTaskBo.getVariables());
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
        String userId = String.valueOf(LoginHelper.getUserId());
        TaskQuery query = taskService.createTaskQuery();
        query.taskTenantId(TenantHelper.getTenantId()).taskCandidateOrAssigned(userId);
        if (CollUtil.isNotEmpty(roles)) {
            List<String> groupIds = StreamUtils.toList(roles, e -> String.valueOf(e.getRoleId()));
            query.taskCandidateGroupIn(groupIds);
        }
        if (StringUtils.isNotBlank(taskBo.getName())) {
            query.taskNameLike("%" + taskBo.getName() + "%");
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
        String userId = String.valueOf(LoginHelper.getUserId());
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
            .taskAssignee(userId).taskTenantId(TenantHelper.getTenantId()).finished().orderByHistoricTaskInstanceStartTime().asc();
        if (StringUtils.isNotBlank(taskBo.getName())) {
            query.taskNameLike("%" + taskBo.getName() + "%");
        }
        List<HistoricTaskInstance> taskInstanceList = query.listPage(taskBo.getPageNum(), taskBo.getPageSize());
        List<TaskVo> list = new ArrayList<>();
        for (HistoricTaskInstance task : taskInstanceList) {
            list.add(BeanUtil.toBean(task, TaskVo.class));
        }
        long count = query.count();
        return new TableDataInfo<>(list, count);
    }

    /**
     * 委派任务
     *
     * @param delegateBo 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delegateTask(DelegateBo delegateBo) {
        TaskEntity task = (TaskEntity) taskService.createTaskQuery()
            .taskTenantId(TenantHelper.getTenantId()).taskId(delegateBo.getTaskId())
            .taskCandidateOrAssigned(String.valueOf(LoginHelper.getUserId())).singleResult();
        if (ObjectUtil.isEmpty(task)) {
            throw new ServiceException(FlowConstant.MESSAGE_CURRENT_TASK_IS_NULL);
        }
        if (task.isSuspended()) {
            throw new ServiceException(FlowConstant.MESSAGE_SUSPENDED);
        }
        try {
            TaskEntity newTask = WorkflowUtils.createNewTask(task);
            taskService.addComment(newTask.getId(), task.getProcessInstanceId(), "【" + LoginHelper.getUsername() + "】委派给【" + delegateBo.getNickName() + "】");
            //委托任务
            taskService.delegateTask(delegateBo.getTaskId(), delegateBo.getUserId());
            //办理生成的任务记录
            taskService.complete(newTask.getId());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 终止任务
     *
     * @param terminationBo 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean terminationTask(TerminationBo terminationBo) {
        Task task = taskService.createTaskQuery()
            .taskTenantId(TenantHelper.getTenantId()).taskId(terminationBo.getTaskId()).singleResult();

        if (ObjectUtil.isEmpty(task)) {
            throw new ServiceException(FlowConstant.MESSAGE_CURRENT_TASK_IS_NULL);
        }
        if (task.isSuspended()) {
            throw new ServiceException(FlowConstant.MESSAGE_SUSPENDED);
        }
        try {
            if (StringUtils.isBlank(terminationBo.getComment())) {
                terminationBo.setComment(LoginHelper.getUsername() + "终止了申请");
            } else {
                terminationBo.setComment(LoginHelper.getUsername() + "终止了申请：" + terminationBo.getComment());
            }
            taskService.addComment(task.getId(), task.getProcessInstanceId(), terminationBo.getComment());
            List<Task> list = taskService.createTaskQuery().taskTenantId(TenantHelper.getTenantId())
                .processInstanceId(task.getProcessInstanceId()).list();
            if (CollectionUtil.isNotEmpty(list)) {
                List<Task> subTasks = StreamUtils.filter(list, e -> StringUtils.isNotBlank(e.getParentTaskId()));
                if (CollectionUtil.isNotEmpty(subTasks)) {
                    subTasks.forEach(e -> taskService.deleteTask(e.getId()));
                }
                runtimeService.deleteProcessInstance(task.getProcessInstanceId(), StrUtil.EMPTY);
            }
            return true;
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 转办任务
     *
     * @param transmitBo 参数
     */
    @Override
    public boolean transferTask(TransmitBo transmitBo) {
        Task task = taskService.createTaskQuery().taskId(transmitBo.getTaskId()).taskTenantId(TenantHelper.getTenantId())
            .taskCandidateOrAssigned(String.valueOf(LoginHelper.getUserId())).singleResult();
        if (ObjectUtil.isEmpty(task)) {
            throw new ServiceException(FlowConstant.MESSAGE_CURRENT_TASK_IS_NULL);
        }
        if (task.isSuspended()) {
            throw new ServiceException(FlowConstant.MESSAGE_SUSPENDED);
        }
        try {
            TaskEntity newTask = WorkflowUtils.createNewTask(task);
            taskService.addComment(newTask.getId(), task.getProcessInstanceId(),
                StringUtils.isNotBlank(transmitBo.getComment()) ? transmitBo.getComment() : LoginHelper.getUsername() + "转办了任务");
            taskService.complete(newTask.getId());
            taskService.setAssignee(task.getId(), transmitBo.getUserId());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }
}
