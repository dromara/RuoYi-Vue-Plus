package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.dto.RoleDTO;
import org.dromara.common.core.domain.dto.UserDTO;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.service.OssService;
import org.dromara.common.core.service.UserService;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.workflow.common.constant.FlowConstant;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.workflow.common.enums.TaskStatusEnum;
import org.dromara.workflow.domain.ActHiTaskinst;
import org.dromara.workflow.domain.WfTaskBackNode;
import org.dromara.workflow.domain.bo.*;
import org.dromara.workflow.domain.vo.*;
import org.dromara.workflow.flowable.cmd.*;
import org.dromara.workflow.flowable.handler.FlowProcessEventHandler;
import org.dromara.workflow.mapper.ActHiTaskinstMapper;
import org.dromara.workflow.mapper.ActTaskMapper;
import org.dromara.workflow.service.IActTaskService;
import org.dromara.workflow.service.IWfDefinitionConfigService;
import org.dromara.workflow.service.IWfNodeConfigService;
import org.dromara.workflow.service.IWfTaskBackNodeService;
import org.dromara.workflow.utils.ModelUtils;
import org.dromara.workflow.utils.QueryUtils;
import org.dromara.workflow.utils.WorkflowUtils;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;
import org.flowable.engine.impl.bpmn.behavior.SequentialMultiInstanceBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.dromara.workflow.common.constant.FlowConstant.*;

/**
 * 任务 服务层实现
 *
 * @author may
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ActTaskServiceImpl implements IActTaskService {

    @Autowired(required = false)
    private RuntimeService runtimeService;
    @Autowired(required = false)
    private TaskService taskService;
    @Autowired(required = false)
    private HistoryService historyService;
    @Autowired(required = false)
    private IdentityService identityService;
    @Autowired(required = false)
    private ManagementService managementService;
    private final ActTaskMapper actTaskMapper;
    private final IWfTaskBackNodeService wfTaskBackNodeService;
    private final ActHiTaskinstMapper actHiTaskinstMapper;
    private final IWfNodeConfigService wfNodeConfigService;
    private final IWfDefinitionConfigService wfDefinitionConfigService;
    private final FlowProcessEventHandler flowProcessEventHandler;
    private final UserService userService;
    private final OssService ossService;

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
        HistoricProcessInstanceQuery query = QueryUtils.hisInstanceQuery();
        HistoricProcessInstance historicProcessInstance = query.processInstanceBusinessKey(startProcessBo.getBusinessKey()).singleResult();
        if (ObjectUtil.isNotEmpty(historicProcessInstance)) {
            BusinessStatusEnum.checkStartStatus(historicProcessInstance.getBusinessStatus());
        }
        List<Task> taskResult = QueryUtils.taskQuery().processInstanceBusinessKey(startProcessBo.getBusinessKey()).list();
        if (CollUtil.isNotEmpty(taskResult)) {
            if (CollUtil.isNotEmpty(startProcessBo.getVariables())) {
                taskService.setVariables(taskResult.get(0).getId(), startProcessBo.getVariables());
            }
            map.put(PROCESS_INSTANCE_ID, taskResult.get(0).getProcessInstanceId());
            map.put("taskId", taskResult.get(0).getId());
            return map;
        }
        WfDefinitionConfigVo wfDefinitionConfigVo = wfDefinitionConfigService.getByTableNameLastVersion(startProcessBo.getTableName());
        if (wfDefinitionConfigVo == null) {
            throw new ServiceException("请到流程定义绑定业务表名与流程KEY！");
        }
        // 设置启动人
        identityService.setAuthenticatedUserId(String.valueOf(LoginHelper.getUserId()));
        Authentication.setAuthenticatedUserId(String.valueOf(LoginHelper.getUserId()));
        // 启动流程实例（提交申请）
        Map<String, Object> variables = startProcessBo.getVariables();
        // 启动跳过表达式
        variables.put(FLOWABLE_SKIP_EXPRESSION_ENABLED, true);
        // 流程发起人
        variables.put(INITIATOR, (String.valueOf(LoginHelper.getUserId())));
        ProcessInstance pi;
        try {
            if (TenantHelper.isEnable()) {
                pi = runtimeService.startProcessInstanceByKeyAndTenantId(wfDefinitionConfigVo.getProcessKey(), startProcessBo.getBusinessKey(), variables, TenantHelper.getTenantId());
            } else {
                pi = runtimeService.startProcessInstanceByKey(wfDefinitionConfigVo.getProcessKey(), startProcessBo.getBusinessKey(), variables);
            }
        } catch (FlowableObjectNotFoundException e) {
            throw new ServiceException("找不到当前【" + wfDefinitionConfigVo.getProcessKey() + "】流程定义！");
        }
        // 将流程定义名称 作为 流程实例名称
        runtimeService.setProcessInstanceName(pi.getProcessInstanceId(), pi.getProcessDefinitionName());
        // 申请人执行流程
        List<Task> taskList = QueryUtils.taskQuery(pi.getId()).list();
        if (taskList.size() > 1) {
            throw new ServiceException("请检查流程第一个环节是否为申请人！");
        }

        runtimeService.updateBusinessStatus(pi.getProcessInstanceId(), BusinessStatusEnum.DRAFT.getStatus());
        taskService.setAssignee(taskList.get(0).getId(), LoginHelper.getUserId().toString());
        taskService.setVariable(taskList.get(0).getId(), PROCESS_INSTANCE_ID, pi.getProcessInstanceId());
        taskService.setVariable(taskList.get(0).getId(), BUSINESS_KEY, pi.getBusinessKey());
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
            String userId = String.valueOf(LoginHelper.getUserId());
            Task task = WorkflowUtils.getTaskByCurrentUser(completeTaskBo.getTaskId());
            if (task == null) {
                throw new ServiceException(FlowConstant.MESSAGE_CURRENT_TASK_IS_NULL);
            }
            if (task.isSuspended()) {
                throw new ServiceException(FlowConstant.MESSAGE_SUSPENDED);
            }
            ProcessInstance processInstance = QueryUtils.instanceQuery(task.getProcessInstanceId()).singleResult();
            //办理委托任务
            if (ObjectUtil.isNotEmpty(task.getDelegationState()) && FlowConstant.PENDING.equals(task.getDelegationState().name())) {
                taskService.resolveTask(completeTaskBo.getTaskId());
                TaskEntity newTask = WorkflowUtils.createNewTask(task);
                taskService.addComment(newTask.getId(), task.getProcessInstanceId(), TaskStatusEnum.PASS.getStatus(), StringUtils.isNotBlank(completeTaskBo.getMessage()) ? completeTaskBo.getMessage() : StrUtil.EMPTY);
                taskService.complete(newTask.getId());
                return true;
            }
            //附件上传
            AttachmentCmd attachmentCmd = new AttachmentCmd(completeTaskBo.getFileId(), task.getId(), task.getProcessInstanceId(), ossService);
            managementService.executeCommand(attachmentCmd);
            String businessStatus = WorkflowUtils.getBusinessStatus(processInstance.getBusinessKey());
            //流程提交监听
            if (BusinessStatusEnum.DRAFT.getStatus().equals(businessStatus) || BusinessStatusEnum.BACK.getStatus().equals(businessStatus) || BusinessStatusEnum.CANCEL.getStatus().equals(businessStatus)) {
                flowProcessEventHandler.processHandler(processInstance.getProcessDefinitionKey(), processInstance.getBusinessKey(), businessStatus, true);
            }
            runtimeService.updateBusinessStatus(task.getProcessInstanceId(), BusinessStatusEnum.WAITING.getStatus());
            //办理监听
            flowProcessEventHandler.processTaskHandler(processInstance.getProcessDefinitionKey(), task.getTaskDefinitionKey(),
                task.getId(), processInstance.getBusinessKey());
            //办理意见
            taskService.addComment(completeTaskBo.getTaskId(), task.getProcessInstanceId(), TaskStatusEnum.PASS.getStatus(), StringUtils.isBlank(completeTaskBo.getMessage()) ? "同意" : completeTaskBo.getMessage());
            //办理任务
            taskService.setAssignee(task.getId(), userId);
            if (CollUtil.isNotEmpty(completeTaskBo.getVariables())) {
                taskService.complete(completeTaskBo.getTaskId(), completeTaskBo.getVariables());
            } else {
                taskService.complete(completeTaskBo.getTaskId());
            }
            //记录执行过的流程任务节点
            wfTaskBackNodeService.recordExecuteNode(task);
            ProcessInstance pi = QueryUtils.instanceQuery(task.getProcessInstanceId()).singleResult();
            if (pi == null) {
                UpdateBusinessStatusCmd updateBusinessStatusCmd = new UpdateBusinessStatusCmd(task.getProcessInstanceId(), BusinessStatusEnum.FINISH.getStatus());
                managementService.executeCommand(updateBusinessStatusCmd);
                flowProcessEventHandler.processHandler(processInstance.getProcessDefinitionKey(), processInstance.getBusinessKey(),
                    BusinessStatusEnum.FINISH.getStatus(), false);
            } else {
                List<Task> list = QueryUtils.taskQuery(task.getProcessInstanceId()).list();
                for (Task t : list) {
                    if (ModelUtils.isUserTask(t.getProcessDefinitionId(), t.getTaskDefinitionKey())) {
                        List<HistoricIdentityLink> links = historyService.getHistoricIdentityLinksForTask(t.getId());
                        if (CollUtil.isEmpty(links) && StringUtils.isBlank(t.getAssignee())) {
                            throw new ServiceException("下一节点【" + t.getName() + "】没有办理人!");
                        }
                    }
                }

                if (CollUtil.isNotEmpty(list) && CollUtil.isNotEmpty(completeTaskBo.getWfCopyList())) {
                    TaskEntity newTask = WorkflowUtils.createNewTask(task);
                    taskService.addComment(newTask.getId(), task.getProcessInstanceId(), TaskStatusEnum.COPY.getStatus(), LoginHelper.getLoginUser().getNickname() + "【抄送】给" + String.join(",", StreamUtils.toList(completeTaskBo.getWfCopyList(), WfCopy::getUserName)));
                    taskService.complete(newTask.getId());
                    List<Task> taskList = QueryUtils.taskQuery(task.getProcessInstanceId()).list();
                    WorkflowUtils.createCopyTask(taskList, StreamUtils.toList(completeTaskBo.getWfCopyList(), WfCopy::getUserId));
                }
                sendMessage(list, processInstance.getName(), completeTaskBo.getMessageType(), null);
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 发送消息
     *
     * @param list        任务
     * @param name        流程名称
     * @param messageType 消息类型
     * @param message     消息内容，为空则发送默认配置的消息内容
     */
    @Async
    public void sendMessage(List<Task> list, String name, List<String> messageType, String message) {
        WorkflowUtils.sendMessage(list, name, messageType, message, userService);
    }

    /**
     * 查询当前用户的待办任务
     *
     * @param taskBo 参数
     */
    @Override
    public TableDataInfo<TaskVo> getPageByTaskWait(TaskBo taskBo, PageQuery pageQuery) {
        QueryWrapper<TaskVo> queryWrapper = new QueryWrapper<>();
        List<RoleDTO> roles = LoginHelper.getLoginUser().getRoles();
        List<String> roleIds = StreamUtils.toList(roles, e -> String.valueOf(e.getRoleId()));
        String userId = String.valueOf(LoginHelper.getUserId());
        queryWrapper.eq("t.business_status_", BusinessStatusEnum.WAITING.getStatus());
        queryWrapper.eq(TenantHelper.isEnable(), "t.tenant_id_", TenantHelper.getTenantId());
        String ids = StreamUtils.join(roleIds, x -> "'" + x + "'");
        queryWrapper.and(w1 -> w1.eq("t.assignee_", userId).or(w2 -> w2.isNull("t.assignee_").apply("exists ( select LINK.ID_ from ACT_RU_IDENTITYLINK LINK where LINK.TASK_ID_ = t.ID_ and LINK.TYPE_ = 'candidate' and (LINK.USER_ID_ = {0} or ( LINK.GROUP_ID_ IN (" + ids + ") ) ))", userId)));
        if (StringUtils.isNotBlank(taskBo.getName())) {
            queryWrapper.like("t.name_", taskBo.getName());
        }
        if (StringUtils.isNotBlank(taskBo.getProcessDefinitionName())) {
            queryWrapper.like("t.processDefinitionName", taskBo.getProcessDefinitionName());
        }
        if (StringUtils.isNotBlank(taskBo.getProcessDefinitionKey())) {
            queryWrapper.eq("t.processDefinitionKey", taskBo.getProcessDefinitionKey());
        }
        Page<TaskVo> page = actTaskMapper.getTaskWaitByPage(pageQuery.build(), queryWrapper);

        List<TaskVo> taskList = page.getRecords();
        if (CollUtil.isNotEmpty(taskList)) {
            List<String> processDefinitionIds = StreamUtils.toList(taskList, TaskVo::getProcessDefinitionId);
            List<WfNodeConfigVo> wfNodeConfigVoList = wfNodeConfigService.selectByDefIds(processDefinitionIds);
            for (TaskVo task : taskList) {
                task.setBusinessStatusName(BusinessStatusEnum.findByStatus(task.getBusinessStatus()));
                task.setParticipantVo(WorkflowUtils.getCurrentTaskParticipant(task.getId(), userService));
                task.setMultiInstance(WorkflowUtils.isMultiInstance(task.getProcessDefinitionId(), task.getTaskDefinitionKey()) != null);
                if (CollUtil.isNotEmpty(wfNodeConfigVoList)) {
                    wfNodeConfigVoList.stream().filter(e -> e.getDefinitionId().equals(task.getProcessDefinitionId()) && FlowConstant.TRUE.equals(e.getApplyUserTask())).findFirst().ifPresent(task::setWfNodeConfigVo);
                    wfNodeConfigVoList.stream().filter(e -> e.getDefinitionId().equals(task.getProcessDefinitionId()) && e.getNodeId().equals(task.getTaskDefinitionKey()) && FlowConstant.FALSE.equals(e.getApplyUserTask())).findFirst().ifPresent(task::setWfNodeConfigVo);
                }
            }
        }
        return TableDataInfo.build(page);
    }

    /**
     * 查询当前租户所有待办任务
     *
     * @param taskBo 参数
     */
    @Override
    public TableDataInfo<TaskVo> getPageByAllTaskWait(TaskBo taskBo, PageQuery pageQuery) {
        TaskQuery query = QueryUtils.taskQuery();
        if (StringUtils.isNotBlank(taskBo.getName())) {
            query.taskNameLike("%" + taskBo.getName() + "%");
        }
        if (StringUtils.isNotBlank(taskBo.getProcessDefinitionName())) {
            query.processDefinitionNameLike("%" + taskBo.getProcessDefinitionName() + "%");
        }
        if (StringUtils.isNotBlank(taskBo.getProcessDefinitionKey())) {
            query.processDefinitionKey(taskBo.getProcessDefinitionKey());
        }
        query.orderByTaskCreateTime().desc();
        List<Task> taskList = query.listPage(pageQuery.getFirstNum(), pageQuery.getPageSize());
        List<ProcessInstance> processInstanceList = null;
        if (CollUtil.isNotEmpty(taskList)) {
            Set<String> processInstanceIds = StreamUtils.toSet(taskList, Task::getProcessInstanceId);
            processInstanceList = QueryUtils.instanceQuery(processInstanceIds).list();
        }
        List<TaskVo> list = new ArrayList<>();
        if (CollUtil.isNotEmpty(taskList)) {
            List<String> processDefinitionIds = StreamUtils.toList(taskList, Task::getProcessDefinitionId);
            List<WfNodeConfigVo> wfNodeConfigVoList = wfNodeConfigService.selectByDefIds(processDefinitionIds);
            for (Task task : taskList) {
                TaskVo taskVo = BeanUtil.toBean(task, TaskVo.class);
                if (CollUtil.isNotEmpty(processInstanceList)) {
                    processInstanceList.stream().filter(e -> e.getId().equals(task.getProcessInstanceId())).findFirst().ifPresent(e -> {
                        taskVo.setBusinessStatus(e.getBusinessStatus());
                        taskVo.setBusinessStatusName(BusinessStatusEnum.findByStatus(taskVo.getBusinessStatus()));
                        taskVo.setProcessDefinitionKey(e.getProcessDefinitionKey());
                        taskVo.setProcessDefinitionName(e.getProcessDefinitionName());
                        taskVo.setProcessDefinitionVersion(e.getProcessDefinitionVersion());
                        taskVo.setBusinessKey(e.getBusinessKey());
                    });
                }
                taskVo.setAssignee(StringUtils.isNotBlank(task.getAssignee()) ? Long.valueOf(task.getAssignee()) : null);
                taskVo.setParticipantVo(WorkflowUtils.getCurrentTaskParticipant(task.getId(), userService));
                taskVo.setMultiInstance(WorkflowUtils.isMultiInstance(task.getProcessDefinitionId(), task.getTaskDefinitionKey()) != null);
                if (CollUtil.isNotEmpty(wfNodeConfigVoList)) {
                    wfNodeConfigVoList.stream().filter(e -> e.getDefinitionId().equals(task.getProcessDefinitionId()) && FlowConstant.TRUE.equals(e.getApplyUserTask())).findFirst().ifPresent(taskVo::setWfNodeConfigVo);
                    wfNodeConfigVoList.stream().filter(e -> e.getDefinitionId().equals(task.getProcessDefinitionId()) && e.getNodeId().equals(task.getTaskDefinitionKey()) && FlowConstant.FALSE.equals(e.getApplyUserTask())).findFirst().ifPresent(taskVo::setWfNodeConfigVo);
                }
                list.add(taskVo);
            }
        }
        long count = query.count();
        TableDataInfo<TaskVo> build = TableDataInfo.build();
        build.setRows(list);
        build.setTotal(count);
        return build;
    }

    /**
     * 查询当前用户的已办任务
     *
     * @param taskBo 参数
     */
    @Override
    public TableDataInfo<TaskVo> getPageByTaskFinish(TaskBo taskBo, PageQuery pageQuery) {
        String userId = String.valueOf(LoginHelper.getUserId());
        QueryWrapper<TaskVo> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(taskBo.getName()), "t.name_", taskBo.getName());
        queryWrapper.like(StringUtils.isNotBlank(taskBo.getProcessDefinitionName()), "t.processDefinitionName", taskBo.getProcessDefinitionName());
        queryWrapper.eq(StringUtils.isNotBlank(taskBo.getProcessDefinitionKey()), "t.processDefinitionKey", taskBo.getProcessDefinitionKey());
        queryWrapper.eq("t.assignee_", userId);
        Page<TaskVo> page = actTaskMapper.getTaskFinishByPage(pageQuery.build(), queryWrapper);

        List<TaskVo> taskList = page.getRecords();
        if (CollUtil.isNotEmpty(taskList)) {
            List<String> processDefinitionIds = StreamUtils.toList(taskList, TaskVo::getProcessDefinitionId);
            List<WfNodeConfigVo> wfNodeConfigVoList = wfNodeConfigService.selectByDefIds(processDefinitionIds);
            for (TaskVo task : taskList) {
                task.setBusinessStatusName(BusinessStatusEnum.findByStatus(task.getBusinessStatus()));
                if (CollUtil.isNotEmpty(wfNodeConfigVoList)) {
                    wfNodeConfigVoList.stream().filter(e -> e.getDefinitionId().equals(task.getProcessDefinitionId()) && FlowConstant.TRUE.equals(e.getApplyUserTask())).findFirst().ifPresent(task::setWfNodeConfigVo);
                    wfNodeConfigVoList.stream().filter(e -> e.getDefinitionId().equals(task.getProcessDefinitionId()) && e.getNodeId().equals(task.getTaskDefinitionKey()) && FlowConstant.FALSE.equals(e.getApplyUserTask())).findFirst().ifPresent(task::setWfNodeConfigVo);
                }
            }
        }
        return TableDataInfo.build(page);
    }

    /**
     * 查询当前用户的抄送
     *
     * @param taskBo 参数
     */
    @Override
    public TableDataInfo<TaskVo> getPageByTaskCopy(TaskBo taskBo, PageQuery pageQuery) {
        QueryWrapper<TaskVo> queryWrapper = new QueryWrapper<>();
        String userId = String.valueOf(LoginHelper.getUserId());
        if (StringUtils.isNotBlank(taskBo.getName())) {
            queryWrapper.like("t.name_", taskBo.getName());
        }
        if (StringUtils.isNotBlank(taskBo.getProcessDefinitionName())) {
            queryWrapper.like("t.processDefinitionName", taskBo.getProcessDefinitionName());
        }
        if (StringUtils.isNotBlank(taskBo.getProcessDefinitionKey())) {
            queryWrapper.eq("t.processDefinitionKey", taskBo.getProcessDefinitionKey());
        }
        queryWrapper.eq("t.assignee_", userId);
        Page<TaskVo> page = actTaskMapper.getTaskCopyByPage(pageQuery.build(), queryWrapper);

        List<TaskVo> taskList = page.getRecords();
        if (CollUtil.isNotEmpty(taskList)) {
            List<String> processDefinitionIds = StreamUtils.toList(taskList, TaskVo::getProcessDefinitionId);
            List<WfNodeConfigVo> wfNodeConfigVoList = wfNodeConfigService.selectByDefIds(processDefinitionIds);
            for (TaskVo task : taskList) {
                task.setBusinessStatusName(BusinessStatusEnum.findByStatus(task.getBusinessStatus()));
                if (CollUtil.isNotEmpty(wfNodeConfigVoList)) {
                    wfNodeConfigVoList.stream().filter(e -> e.getDefinitionId().equals(task.getProcessDefinitionId()) && FlowConstant.TRUE.equals(e.getApplyUserTask())).findFirst().ifPresent(task::setWfNodeConfigVo);
                    wfNodeConfigVoList.stream().filter(e -> e.getDefinitionId().equals(task.getProcessDefinitionId()) && e.getNodeId().equals(task.getTaskDefinitionKey()) && FlowConstant.FALSE.equals(e.getApplyUserTask())).findFirst().ifPresent(task::setWfNodeConfigVo);
                }
            }
        }
        return TableDataInfo.build(page);
    }

    /**
     * 查询当前租户所有已办任务
     *
     * @param taskBo 参数
     */
    @Override
    public TableDataInfo<TaskVo> getPageByAllTaskFinish(TaskBo taskBo, PageQuery pageQuery) {
        QueryWrapper<TaskVo> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(taskBo.getName()), "t.name_", taskBo.getName());
        queryWrapper.like(StringUtils.isNotBlank(taskBo.getProcessDefinitionName()), "t.processDefinitionName", taskBo.getProcessDefinitionName());
        queryWrapper.eq(StringUtils.isNotBlank(taskBo.getProcessDefinitionKey()), "t.processDefinitionKey", taskBo.getProcessDefinitionKey());
        Page<TaskVo> page = actTaskMapper.getTaskFinishByPage(pageQuery.build(), queryWrapper);

        List<TaskVo> taskList = page.getRecords();
        if (CollUtil.isNotEmpty(taskList)) {
            List<String> processDefinitionIds = StreamUtils.toList(taskList, TaskVo::getProcessDefinitionId);
            List<WfNodeConfigVo> wfNodeConfigVoList = wfNodeConfigService.selectByDefIds(processDefinitionIds);
            for (TaskVo task : taskList) {
                task.setBusinessStatusName(BusinessStatusEnum.findByStatus(task.getBusinessStatus()));
                if (CollUtil.isNotEmpty(wfNodeConfigVoList)) {
                    wfNodeConfigVoList.stream().filter(e -> e.getDefinitionId().equals(task.getProcessDefinitionId()) && FlowConstant.TRUE.equals(e.getApplyUserTask())).findFirst().ifPresent(task::setWfNodeConfigVo);
                    wfNodeConfigVoList.stream().filter(e -> e.getDefinitionId().equals(task.getProcessDefinitionId()) && e.getNodeId().equals(task.getTaskDefinitionKey()) && FlowConstant.FALSE.equals(e.getApplyUserTask())).findFirst().ifPresent(task::setWfNodeConfigVo);
                }
            }
        }
        return TableDataInfo.build(page);
    }

    /**
     * 委派任务
     *
     * @param delegateBo 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delegateTask(DelegateBo delegateBo) {
        Task task = WorkflowUtils.getTaskByCurrentUser(delegateBo.getTaskId());

        if (ObjectUtil.isEmpty(task)) {
            throw new ServiceException(FlowConstant.MESSAGE_CURRENT_TASK_IS_NULL);
        }
        if (task.isSuspended()) {
            throw new ServiceException(FlowConstant.MESSAGE_SUSPENDED);
        }
        try {
            TaskEntity newTask = WorkflowUtils.createNewTask(task);
            taskService.addComment(newTask.getId(), task.getProcessInstanceId(), TaskStatusEnum.PENDING.getStatus(), "【" + LoginHelper.getLoginUser().getNickname() + "】委派给【" + delegateBo.getNickName() + "】");
            //委托任务
            taskService.delegateTask(delegateBo.getTaskId(), delegateBo.getUserId());
            //办理生成的任务记录
            taskService.complete(newTask.getId());
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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
        TaskQuery query = QueryUtils.taskQuery();
        Task task = query.taskId(terminationBo.getTaskId()).singleResult();

        if (ObjectUtil.isEmpty(task)) {
            throw new ServiceException(FlowConstant.MESSAGE_CURRENT_TASK_IS_NULL);
        }
        if (task.isSuspended()) {
            throw new ServiceException(FlowConstant.MESSAGE_SUSPENDED);
        }
        HistoricProcessInstance historicProcessInstance = QueryUtils.hisInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
        BusinessStatusEnum.checkInvalidStatus(historicProcessInstance.getBusinessStatus());
        try {
            if (StringUtils.isBlank(terminationBo.getComment())) {
                terminationBo.setComment(LoginHelper.getLoginUser().getNickname() + "终止了申请");
            } else {
                terminationBo.setComment(LoginHelper.getLoginUser().getNickname() + "终止了申请：" + terminationBo.getComment());
            }
            taskService.addComment(task.getId(), task.getProcessInstanceId(), TaskStatusEnum.TERMINATION.getStatus(), terminationBo.getComment());
            List<Task> list = QueryUtils.taskQuery(task.getProcessInstanceId()).list();
            if (CollUtil.isNotEmpty(list)) {
                List<Task> subTasks = StreamUtils.filter(list, e -> StringUtils.isNotBlank(e.getParentTaskId()));
                if (CollUtil.isNotEmpty(subTasks)) {
                    subTasks.forEach(e -> taskService.deleteTask(e.getId()));
                }
                runtimeService.updateBusinessStatus(task.getProcessInstanceId(), BusinessStatusEnum.TERMINATION.getStatus());
                runtimeService.deleteProcessInstance(task.getProcessInstanceId(), StrUtil.EMPTY);
            }
            //流程终止监听
            flowProcessEventHandler.processHandler(historicProcessInstance.getProcessDefinitionKey(),
                historicProcessInstance.getBusinessKey(), BusinessStatusEnum.TERMINATION.getStatus(), false);
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
        Task task = WorkflowUtils.getTaskByCurrentUser(transmitBo.getTaskId());
        if (ObjectUtil.isEmpty(task)) {
            throw new ServiceException(FlowConstant.MESSAGE_CURRENT_TASK_IS_NULL);
        }
        if (task.isSuspended()) {
            throw new ServiceException(FlowConstant.MESSAGE_SUSPENDED);
        }
        try {
            TaskEntity newTask = WorkflowUtils.createNewTask(task);
            taskService.addComment(newTask.getId(), task.getProcessInstanceId(), TaskStatusEnum.TRANSFER.getStatus(), StringUtils.isNotBlank(transmitBo.getComment()) ? transmitBo.getComment() : LoginHelper.getUsername() + "转办了任务");
            taskService.complete(newTask.getId());
            taskService.setAssignee(task.getId(), transmitBo.getUserId());
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 会签任务加签
     *
     * @param addMultiBo 参数
     */
    @Override
    public boolean addMultiInstanceExecution(AddMultiBo addMultiBo) {
        TaskQuery taskQuery = QueryUtils.taskQuery();
        taskQuery.taskId(addMultiBo.getTaskId());
        if (!LoginHelper.isSuperAdmin() && !LoginHelper.isTenantAdmin()) {
            taskQuery.taskCandidateOrAssigned(String.valueOf(LoginHelper.getUserId()));
        }
        Task task = taskQuery.singleResult();
        if (ObjectUtil.isEmpty(task)) {
            throw new ServiceException(FlowConstant.MESSAGE_CURRENT_TASK_IS_NULL);
        }
        if (task.isSuspended()) {
            throw new ServiceException(FlowConstant.MESSAGE_SUSPENDED);
        }
        String taskDefinitionKey = task.getTaskDefinitionKey();
        String processInstanceId = task.getProcessInstanceId();
        String processDefinitionId = task.getProcessDefinitionId();

        try {
            MultiInstanceVo multiInstanceVo = WorkflowUtils.isMultiInstance(processDefinitionId, taskDefinitionKey);
            if (multiInstanceVo == null) {
                throw new ServiceException("当前环节不是会签节点");
            }
            if (multiInstanceVo.getType() instanceof ParallelMultiInstanceBehavior) {
                for (Long assignee : addMultiBo.getAssignees()) {
                    runtimeService.addMultiInstanceExecution(taskDefinitionKey, processInstanceId, Collections.singletonMap(multiInstanceVo.getAssignee(), assignee));
                }
            } else if (multiInstanceVo.getType() instanceof SequentialMultiInstanceBehavior) {
                AddSequenceMultiInstanceCmd addSequenceMultiInstanceCmd = new AddSequenceMultiInstanceCmd(task.getExecutionId(), multiInstanceVo.getAssigneeList(), addMultiBo.getAssignees());
                managementService.executeCommand(addSequenceMultiInstanceCmd);
            }
            List<String> assigneeNames = addMultiBo.getAssigneeNames();
            String username = LoginHelper.getUsername();
            TaskEntity newTask = WorkflowUtils.createNewTask(task);
            taskService.addComment(newTask.getId(), processInstanceId, TaskStatusEnum.SIGN.getStatus(), username + "加签【" + String.join(StringUtils.SEPARATOR, assigneeNames) + "】");
            taskService.complete(newTask.getId());
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 会签任务减签
     *
     * @param deleteMultiBo 参数
     */
    @Override
    public boolean deleteMultiInstanceExecution(DeleteMultiBo deleteMultiBo) {
        TaskQuery taskQuery = QueryUtils.taskQuery();
        taskQuery.taskId(deleteMultiBo.getTaskId());
        if (!LoginHelper.isSuperAdmin() && !LoginHelper.isTenantAdmin()) {
            taskQuery.taskCandidateOrAssigned(String.valueOf(LoginHelper.getUserId()));
        }
        Task task = taskQuery.singleResult();
        if (ObjectUtil.isEmpty(task)) {
            throw new ServiceException(FlowConstant.MESSAGE_CURRENT_TASK_IS_NULL);
        }
        if (task.isSuspended()) {
            throw new ServiceException(FlowConstant.MESSAGE_SUSPENDED);
        }
        String taskDefinitionKey = task.getTaskDefinitionKey();
        String processInstanceId = task.getProcessInstanceId();
        String processDefinitionId = task.getProcessDefinitionId();
        try {
            MultiInstanceVo multiInstanceVo = WorkflowUtils.isMultiInstance(processDefinitionId, taskDefinitionKey);
            if (multiInstanceVo == null) {
                throw new ServiceException("当前环节不是会签节点");
            }
            if (multiInstanceVo.getType() instanceof ParallelMultiInstanceBehavior) {
                for (String executionId : deleteMultiBo.getExecutionIds()) {
                    runtimeService.deleteMultiInstanceExecution(executionId, false);
                }
                for (String taskId : deleteMultiBo.getTaskIds()) {
                    historyService.deleteHistoricTaskInstance(taskId);
                }
            } else if (multiInstanceVo.getType() instanceof SequentialMultiInstanceBehavior) {
                DeleteSequenceMultiInstanceCmd deleteSequenceMultiInstanceCmd = new DeleteSequenceMultiInstanceCmd(task.getAssignee(), task.getExecutionId(), multiInstanceVo.getAssigneeList(), deleteMultiBo.getAssigneeIds());
                managementService.executeCommand(deleteSequenceMultiInstanceCmd);
            }
            List<String> assigneeNames = deleteMultiBo.getAssigneeNames();
            String username = LoginHelper.getUsername();
            TaskEntity newTask = WorkflowUtils.createNewTask(task);
            taskService.addComment(newTask.getId(), processInstanceId, TaskStatusEnum.SIGN_OFF.getStatus(), username + "减签【" + String.join(StringUtils.SEPARATOR, assigneeNames) + "】");
            taskService.complete(newTask.getId());
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 驳回审批
     *
     * @param backProcessBo 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String backProcess(BackProcessBo backProcessBo) {
        String userId = String.valueOf(LoginHelper.getUserId());
        Task task = WorkflowUtils.getTaskByCurrentUser(backProcessBo.getTaskId());

        if (ObjectUtil.isEmpty(task)) {
            throw new ServiceException(FlowConstant.MESSAGE_CURRENT_TASK_IS_NULL);
        }
        if (task.isSuspended()) {
            throw new ServiceException(FlowConstant.MESSAGE_SUSPENDED);
        }
        try {
            String processInstanceId = task.getProcessInstanceId();
            ProcessInstance processInstance = QueryUtils.instanceQuery(task.getProcessInstanceId()).singleResult();
            //获取并行网关执行后保留的执行实例数据
            ExecutionChildByExecutionIdCmd childByExecutionIdCmd = new ExecutionChildByExecutionIdCmd(task.getExecutionId());
            List<ExecutionEntity> executionEntities = managementService.executeCommand(childByExecutionIdCmd);
            //校验单据
            BusinessStatusEnum.checkBackStatus(processInstance.getBusinessStatus());
            //判断是否有多个任务
            List<Task> taskList = QueryUtils.taskQuery(processInstanceId).list();
            String backTaskDefinitionKey = backProcessBo.getTargetActivityId();
            taskService.addComment(task.getId(), processInstanceId, TaskStatusEnum.BACK.getStatus(), StringUtils.isNotBlank(backProcessBo.getMessage()) ? backProcessBo.getMessage() : "退回");
            if (taskList.size() > 1) {
                //当前多个任务驳回到单个节点
                runtimeService.createChangeActivityStateBuilder().processInstanceId(processInstanceId).moveActivityIdsToSingleActivityId(taskList.stream().map(Task::getTaskDefinitionKey).distinct().collect(Collectors.toList()), backTaskDefinitionKey).changeState();
                ActHiTaskinst actHiTaskinst = new ActHiTaskinst();
                actHiTaskinst.setAssignee(userId);
                actHiTaskinst.setId(task.getId());
                actHiTaskinstMapper.updateById(actHiTaskinst);
            } else {
                //当前单个节点驳回单个节点
                runtimeService.createChangeActivityStateBuilder().processInstanceId(processInstanceId).moveActivityIdTo(task.getTaskDefinitionKey(), backTaskDefinitionKey).changeState();
            }
            //删除并行环节未办理记录
            MultiInstanceVo multiInstance = WorkflowUtils.isMultiInstance(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
            if (multiInstance == null && taskList.size() > 1) {
                List<Task> tasks = StreamUtils.filter(taskList, e -> !e.getTaskDefinitionKey().equals(task.getTaskDefinitionKey()));
                if (CollUtil.isNotEmpty(tasks)) {
                    actHiTaskinstMapper.deleteByIds(StreamUtils.toList(tasks, Task::getId));
                }
            }


            List<HistoricTaskInstance> instanceList = QueryUtils.hisTaskInstanceQuery(processInstanceId).finished().orderByHistoricTaskInstanceEndTime().desc().list();
            List<Task> list = QueryUtils.taskQuery(processInstanceId).list();
            for (Task t : list) {
                instanceList.stream().filter(e -> e.getTaskDefinitionKey().equals(t.getTaskDefinitionKey())).findFirst().ifPresent(e -> {
                    taskService.setAssignee(t.getId(), e.getAssignee());
                });
            }
            //发送消息
            String message = "您的【" + processInstance.getName() + "】单据已经被驳回，请您注意查收。";
            sendMessage(list, processInstance.getName(), backProcessBo.getMessageType(), message);
            //删除流程实例垃圾数据
            for (ExecutionEntity executionEntity : executionEntities) {
                DeleteExecutionCmd deleteExecutionCmd = new DeleteExecutionCmd(executionEntity.getId());
                managementService.executeCommand(deleteExecutionCmd);
            }

            WfTaskBackNode wfTaskBackNode = wfTaskBackNodeService.getListByInstanceIdAndNodeId(task.getProcessInstanceId(), backProcessBo.getTargetActivityId());
            if (ObjectUtil.isNotNull(wfTaskBackNode) && wfTaskBackNode.getOrderNo() == 0) {
                runtimeService.updateBusinessStatus(processInstanceId, BusinessStatusEnum.BACK.getStatus());
                flowProcessEventHandler.processHandler(processInstance.getProcessDefinitionKey(),
                    processInstance.getBusinessKey(), BusinessStatusEnum.BACK.getStatus(), false);
            }
            //删除驳回后的流程节点
            wfTaskBackNodeService.deleteBackTaskNode(processInstanceId, backProcessBo.getTargetActivityId());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
        return task.getProcessInstanceId();
    }

    /**
     * 修改任务办理人
     *
     * @param taskIds 任务id
     * @param userId  办理人id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAssignee(String[] taskIds, String userId) {
        try {
            List<Task> list = QueryUtils.taskQuery().taskIds(Arrays.asList(taskIds)).list();
            for (Task task : list) {
                taskService.setAssignee(task.getId(), userId);
            }
        } catch (Exception e) {
            log.error("修改失败：" + e.getMessage(), e);
            throw new ServiceException("修改失败：" + e.getMessage());
        }
        return true;
    }

    /**
     * 查询流程变量
     *
     * @param taskId 任务id
     */
    @Override
    public List<VariableVo> getInstanceVariable(String taskId) {
        List<VariableVo> variableVoList = new ArrayList<>();
        Map<String, VariableInstance> variableInstances = taskService.getVariableInstances(taskId);
        if (CollUtil.isNotEmpty(variableInstances)) {
            for (Map.Entry<String, VariableInstance> entry : variableInstances.entrySet()) {
                VariableVo variableVo = new VariableVo();
                variableVo.setKey(entry.getKey());
                variableVo.setValue(entry.getValue().getValue().toString());
                variableVoList.add(variableVo);
            }
        }
        return variableVoList;
    }

    /**
     * 查询工作流任务用户选择加签人员
     *
     * @param taskId 任务id
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public String getTaskUserIdsByAddMultiInstance(String taskId) {
        Task task = QueryUtils.taskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new ServiceException("任务不存在");
        }
        MultiInstanceVo multiInstance = WorkflowUtils.isMultiInstance(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
        if (multiInstance == null) {
            return "";
        }
        List<Long> userIds;
        if (multiInstance.getType() instanceof SequentialMultiInstanceBehavior) {
            userIds = (List<Long>) runtimeService.getVariable(task.getExecutionId(), multiInstance.getAssigneeList());
        } else {
            List<Task> list = QueryUtils.taskQuery(task.getProcessInstanceId()).list();
            userIds = StreamUtils.toList(list, e -> Long.valueOf(e.getAssignee()));
        }
        return StringUtils.join(userIds, StringUtils.SEPARATOR);
    }

    /**
     * 查询工作流选择减签人员
     *
     * @param taskId 任务id 任务id
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<TaskVo> getListByDeleteMultiInstance(String taskId) {
        Task task = QueryUtils.taskQuery().taskId(taskId).singleResult();
        List<Task> taskList = QueryUtils.taskQuery(task.getProcessInstanceId()).list();
        MultiInstanceVo multiInstance = WorkflowUtils.isMultiInstance(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
        List<TaskVo> taskListVo = new ArrayList<>();
        if (multiInstance == null) {
            return List.of();
        }
        List<Long> assigneeList = new ArrayList<>();
        if (multiInstance.getType() instanceof SequentialMultiInstanceBehavior) {
            List<Object> variable = (List<Object>) runtimeService.getVariable(task.getExecutionId(), multiInstance.getAssigneeList());
            for (Object o : variable) {
                assigneeList.add(Long.valueOf(o.toString()));
            }
        }

        if (multiInstance.getType() instanceof SequentialMultiInstanceBehavior) {
            List<Long> userIds = StreamUtils.filter(assigneeList, e -> !String.valueOf(e).equals(task.getAssignee()));
            List<UserDTO> userList = userService.selectListByIds(userIds);
            for (Long userId : userIds) {
                TaskVo taskVo = new TaskVo();
                taskVo.setId("串行会签");
                taskVo.setExecutionId("串行会签");
                taskVo.setProcessInstanceId(task.getProcessInstanceId());
                taskVo.setName(task.getName());
                taskVo.setAssignee(userId);
                if (CollUtil.isNotEmpty(userList)) {
                    userList.stream().filter(u -> u.getUserId().toString().equals(userId.toString())).findFirst().ifPresent(u -> taskVo.setAssigneeName(u.getNickName()));
                }
                taskListVo.add(taskVo);
            }
            return taskListVo;
        } else if (multiInstance.getType() instanceof ParallelMultiInstanceBehavior) {
            List<Task> tasks = StreamUtils.filter(taskList, e -> StringUtils.isBlank(e.getParentTaskId()) && !e.getExecutionId().equals(task.getExecutionId()) && e.getTaskDefinitionKey().equals(task.getTaskDefinitionKey()));
            if (CollUtil.isNotEmpty(tasks)) {
                List<Long> userIds = StreamUtils.toList(tasks, e -> Long.valueOf(e.getAssignee()));
                List<UserDTO> userList = userService.selectListByIds(userIds);
                for (Task t : tasks) {
                    TaskVo taskVo = new TaskVo();
                    taskVo.setId(t.getId());
                    taskVo.setExecutionId(t.getExecutionId());
                    taskVo.setProcessInstanceId(t.getProcessInstanceId());
                    taskVo.setName(t.getName());
                    taskVo.setAssignee(Long.valueOf(t.getAssignee()));
                    if (CollUtil.isNotEmpty(userList)) {
                        userList.stream().filter(u -> u.getUserId().toString().equals(t.getAssignee())).findFirst().ifPresent(e -> taskVo.setAssigneeName(e.getNickName()));
                    }
                    taskListVo.add(taskVo);
                }
                return taskListVo;
            }
        }
        return List.of();
    }
}
