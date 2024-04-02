package org.dromara.workflow.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.reflect.ReflectUtils;
import org.dromara.common.mail.utils.MailUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.common.websocket.dto.WebSocketMessageDto;
import org.dromara.common.websocket.utils.WebSocketUtils;
import org.dromara.system.domain.SysUserRole;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.workflow.common.constant.FlowConstant;
import org.dromara.workflow.common.enums.BusinessStatusEnum;
import org.dromara.workflow.common.enums.MessageTypeEnum;
import org.dromara.workflow.common.enums.TaskStatusEnum;
import org.dromara.workflow.domain.ActHiProcinst;
import org.dromara.workflow.domain.ActHiTaskinst;
import org.dromara.workflow.domain.vo.*;
import org.dromara.workflow.flowable.cmd.UpdateHiTaskInstCmd;
import org.dromara.workflow.mapper.ActHiTaskinstMapper;
import org.dromara.workflow.service.*;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;
import org.flowable.engine.impl.bpmn.behavior.SequentialMultiInstanceBehavior;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

import java.util.*;

import static org.dromara.workflow.common.constant.FlowConstant.*;

/**
 * 工作流工具
 *
 * @author may
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkflowUtils {

    private static final ProcessEngine PROCESS_ENGINE = SpringUtils.getBean(ProcessEngine.class);
    private static final IWorkflowUserService WORKFLOW_USER_SERVICE = SpringUtils.getBean(IWorkflowUserService.class);
    private static final IActHiProcinstService ACT_HI_PROCINST_SERVICE = SpringUtils.getBean(IActHiProcinstService.class);
    private static final ActHiTaskinstMapper ACT_HI_TASKINST_MAPPER = SpringUtils.getBean(ActHiTaskinstMapper.class);
    private static final IWfDefinitionConfigService I_WF_DEFINITION_CONFIG_SERVICE = SpringUtils.getBean(IWfDefinitionConfigService.class);
    private static final IWfFormManageService I_WF_FORM_MANAGE_SERVICE = SpringUtils.getBean(IWfFormManageService.class);

    /**
     * 创建一个新任务
     *
     * @param currentTask 参数
     */
    public static TaskEntity createNewTask(Task currentTask) {
        TaskEntity task = null;
        if (ObjectUtil.isNotEmpty(currentTask)) {
            task = (TaskEntity) PROCESS_ENGINE.getTaskService().newTask();
            task.setCategory(currentTask.getCategory());
            task.setDescription(currentTask.getDescription());
            task.setAssignee(currentTask.getAssignee());
            task.setName(currentTask.getName());
            task.setProcessDefinitionId(currentTask.getProcessDefinitionId());
            task.setProcessInstanceId(currentTask.getProcessInstanceId());
            task.setTaskDefinitionKey(currentTask.getTaskDefinitionKey());
            task.setPriority(currentTask.getPriority());
            task.setCreateTime(new Date());
            task.setTenantId(TenantHelper.getTenantId());
            PROCESS_ENGINE.getTaskService().saveTask(task);
        }
        if (ObjectUtil.isNotNull(task)) {
            UpdateHiTaskInstCmd updateHiTaskInstCmd = new UpdateHiTaskInstCmd(Collections.singletonList(task.getId()), task.getProcessDefinitionId(), task.getProcessInstanceId());
            PROCESS_ENGINE.getManagementService().executeCommand(updateHiTaskInstCmd);
        }
        return task;
    }

    /**
     * 抄送任务
     *
     * @param parentTaskList 父级任务
     * @param userIds        人员id
     */
    public static void createCopyTask(List<Task> parentTaskList, List<Long> userIds) {
        List<Task> list = new ArrayList<>();
        String tenantId = TenantHelper.getTenantId();
        for (Task parentTask : parentTaskList) {
            for (Long userId : userIds) {
                TaskEntity newTask = (TaskEntity) PROCESS_ENGINE.getTaskService().newTask();
                newTask.setParentTaskId(parentTask.getId());
                newTask.setAssignee(userId.toString());
                newTask.setName("【抄送】-" + parentTask.getName());
                newTask.setProcessDefinitionId(parentTask.getProcessDefinitionId());
                newTask.setProcessInstanceId(parentTask.getProcessInstanceId());
                newTask.setTaskDefinitionKey(parentTask.getTaskDefinitionKey());
                newTask.setTenantId(tenantId);
                list.add(newTask);
            }
        }
        PROCESS_ENGINE.getTaskService().bulkSaveTasks(list);
        if (CollUtil.isNotEmpty(list) && CollUtil.isNotEmpty(parentTaskList)) {
            String processInstanceId = parentTaskList.get(0).getProcessInstanceId();
            String processDefinitionId = parentTaskList.get(0).getProcessDefinitionId();
            List<String> taskIds = StreamUtils.toList(list, Task::getId);
            ActHiTaskinst actHiTaskinst = new ActHiTaskinst();
            actHiTaskinst.setProcDefId(processDefinitionId);
            actHiTaskinst.setProcInstId(processInstanceId);
            actHiTaskinst.setScopeType(TaskStatusEnum.COPY.getStatus());
            actHiTaskinst.setTenantId(tenantId);
            LambdaUpdateWrapper<ActHiTaskinst> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.in(ActHiTaskinst::getId, taskIds);
            ACT_HI_TASKINST_MAPPER.update(actHiTaskinst, updateWrapper);
            for (Task task : list) {
                PROCESS_ENGINE.getTaskService().addComment(task.getId(), task.getProcessInstanceId(), TaskStatusEnum.COPY.getStatus(), StrUtil.EMPTY);
            }
        }
    }

    /**
     * 获取当前任务参与者
     *
     * @param taskId 任务id
     */
    public static ParticipantVo getCurrentTaskParticipant(String taskId) {
        ParticipantVo participantVo = new ParticipantVo();
        List<HistoricIdentityLink> linksForTask = PROCESS_ENGINE.getHistoryService().getHistoricIdentityLinksForTask(taskId);
        Task task = QueryUtils.taskQuery().taskId(taskId).singleResult();
        if (task != null && CollUtil.isNotEmpty(linksForTask)) {
            List<HistoricIdentityLink> groupList = StreamUtils.filter(linksForTask, e -> StringUtils.isNotBlank(e.getGroupId()));
            if (CollUtil.isNotEmpty(groupList)) {
                List<Long> groupIds = StreamUtils.toList(groupList, e -> Long.valueOf(e.getGroupId()));
                List<SysUserRole> sysUserRoles = WORKFLOW_USER_SERVICE.getUserRoleListByRoleIds(groupIds);
                if (CollUtil.isNotEmpty(sysUserRoles)) {
                    participantVo.setGroupIds(groupIds);
                    List<Long> userIdList = StreamUtils.toList(sysUserRoles, SysUserRole::getUserId);
                    List<SysUserVo> sysUsers = WORKFLOW_USER_SERVICE.getUserListByIds(userIdList);
                    if (CollUtil.isNotEmpty(sysUsers)) {
                        List<Long> userIds = StreamUtils.toList(sysUsers, SysUserVo::getUserId);
                        List<String> nickNames = StreamUtils.toList(sysUsers, SysUserVo::getNickName);
                        participantVo.setCandidate(userIds);
                        participantVo.setCandidateName(nickNames);
                        participantVo.setClaim(!StringUtils.isBlank(task.getAssignee()));
                    }
                }
            } else {
                List<HistoricIdentityLink> candidateList = StreamUtils.filter(linksForTask, e -> FlowConstant.CANDIDATE.equals(e.getType()));
                List<Long> userIdList = new ArrayList<>();
                for (HistoricIdentityLink historicIdentityLink : linksForTask) {
                    try {
                        userIdList.add(Long.valueOf(historicIdentityLink.getUserId()));
                    } catch (NumberFormatException ignored) {

                    }
                }
                List<SysUserVo> sysUsers = WORKFLOW_USER_SERVICE.getUserListByIds(userIdList);
                if (CollUtil.isNotEmpty(sysUsers)) {
                    List<Long> userIds = StreamUtils.toList(sysUsers, SysUserVo::getUserId);
                    List<String> nickNames = StreamUtils.toList(sysUsers, SysUserVo::getNickName);
                    participantVo.setCandidate(userIds);
                    participantVo.setCandidateName(nickNames);
                    // 判断当前任务是否具有多个办理人
                    if (CollUtil.isNotEmpty(candidateList) && candidateList.size() > 1) {
                        // 如果 assignee 存在，则设置当前任务已经被认领
                        participantVo.setClaim(StringUtils.isNotBlank(task.getAssignee()));
                    }
                }
            }
        }
        return participantVo;
    }

    /**
     * 判断当前节点是否为会签节点
     *
     * @param processDefinitionId 流程定义id
     * @param taskDefinitionKey   流程定义id
     */
    public static MultiInstanceVo isMultiInstance(String processDefinitionId, String taskDefinitionKey) {
        BpmnModel bpmnModel = PROCESS_ENGINE.getRepositoryService().getBpmnModel(processDefinitionId);
        FlowNode flowNode = (FlowNode) bpmnModel.getFlowElement(taskDefinitionKey);
        MultiInstanceVo multiInstanceVo = new MultiInstanceVo();
        //判断是否为并行会签节点
        if (flowNode.getBehavior() instanceof ParallelMultiInstanceBehavior behavior && behavior.getCollectionExpression() != null) {
            Expression collectionExpression = behavior.getCollectionExpression();
            String assigneeList = collectionExpression.getExpressionText();
            String assignee = behavior.getCollectionElementVariable();
            multiInstanceVo.setType(behavior);
            multiInstanceVo.setAssignee(assignee);
            multiInstanceVo.setAssigneeList(assigneeList);
            return multiInstanceVo;
            //判断是否为串行会签节点
        } else if (flowNode.getBehavior() instanceof SequentialMultiInstanceBehavior behavior && behavior.getCollectionExpression() != null) {
            Expression collectionExpression = behavior.getCollectionExpression();
            String assigneeList = collectionExpression.getExpressionText();
            String assignee = behavior.getCollectionElementVariable();
            multiInstanceVo.setType(behavior);
            multiInstanceVo.setAssignee(assignee);
            multiInstanceVo.setAssigneeList(assigneeList);
            return multiInstanceVo;
        }
        return null;
    }

    /**
     * 获取当前流程状态
     *
     * @param taskId 任务id
     */
    public static String getBusinessStatusByTaskId(String taskId) {
        HistoricTaskInstance historicTaskInstance = QueryUtils.hisTaskInstanceQuery().taskId(taskId).singleResult();
        HistoricProcessInstance historicProcessInstance = QueryUtils.hisInstanceQuery(historicTaskInstance.getProcessInstanceId()).singleResult();
        return historicProcessInstance.getBusinessStatus();
    }

    /**
     * 获取当前流程状态
     *
     * @param processInstanceId 流程实例id
     */
    public static String getBusinessStatus(String processInstanceId) {
        HistoricProcessInstance historicProcessInstance = QueryUtils.hisInstanceQuery(processInstanceId).singleResult();
        return historicProcessInstance.getBusinessStatus();
    }

    /**
     * 设置流程实例对象
     *
     * @param obj         业务对象
     * @param businessKey 业务id
     */
    public static void setProcessInstanceVo(Object obj, String businessKey) {
        if (StringUtils.isBlank(businessKey) || obj == null) {
            return;
        }
        ActHiProcinst actHiProcinst = ACT_HI_PROCINST_SERVICE.selectByBusinessKey(businessKey);
        if (actHiProcinst == null) {
            ProcessInstanceVo processInstanceVo = new ProcessInstanceVo();
            processInstanceVo.setBusinessStatus(BusinessStatusEnum.DRAFT.getStatus());
            ReflectUtils.invokeSetter(obj, PROCESS_INSTANCE_VO, processInstanceVo);
            return;
        }
        ProcessInstanceVo processInstanceVo = BeanUtil.toBean(actHiProcinst, ProcessInstanceVo.class);
        processInstanceVo.setBusinessStatusName(BusinessStatusEnum.findByStatus(processInstanceVo.getBusinessStatus()));
        ReflectUtils.invokeSetter(obj, PROCESS_INSTANCE_VO, processInstanceVo);
    }

    /**
     * 设置流程实例对象
     *
     * @param obj       业务对象
     * @param idList    业务id
     * @param fieldName 主键属性名称
     */
    public static void setProcessInstanceListVo(Object obj, List<String> idList, String fieldName) {
        if (CollUtil.isEmpty(idList) || obj == null) {
            return;
        }
        List<ActHiProcinst> actHiProcinstList = ACT_HI_PROCINST_SERVICE.selectByBusinessKeyIn(idList);
        if (obj instanceof Collection<?> collection) {
            for (Object o : collection) {
                String fieldValue = ReflectUtils.invokeGetter(o, fieldName).toString();
                if (CollUtil.isEmpty(actHiProcinstList)) {
                    ProcessInstanceVo processInstanceVo = new ProcessInstanceVo();
                    processInstanceVo.setBusinessStatus(BusinessStatusEnum.DRAFT.getStatus());
                    processInstanceVo.setBusinessStatusName(BusinessStatusEnum.findByStatus(processInstanceVo.getBusinessStatus()));
                    ReflectUtils.invokeSetter(o, PROCESS_INSTANCE_VO, processInstanceVo);
                } else {
                    ActHiProcinst actHiProcinst = actHiProcinstList.stream().filter(e -> e.getBusinessKey().equals(fieldValue)).findFirst().orElse(null);
                    if (ObjectUtil.isNotEmpty(actHiProcinst)) {
                        ProcessInstanceVo processInstanceVo = BeanUtil.toBean(actHiProcinst, ProcessInstanceVo.class);
                        processInstanceVo.setBusinessStatusName(BusinessStatusEnum.findByStatus(processInstanceVo.getBusinessStatus()));
                        ReflectUtils.invokeSetter(o, PROCESS_INSTANCE_VO, processInstanceVo);
                    } else {
                        ProcessInstanceVo processInstanceVo = new ProcessInstanceVo();
                        processInstanceVo.setBusinessStatus(BusinessStatusEnum.DRAFT.getStatus());
                        processInstanceVo.setBusinessStatusName(BusinessStatusEnum.findByStatus(processInstanceVo.getBusinessStatus()));
                        ReflectUtils.invokeSetter(o, PROCESS_INSTANCE_VO, processInstanceVo);
                    }
                }
            }
        }
    }

    /**
     * 设置流程流程定义配置
     *
     * @param obj       业务对象
     * @param idList    流程定义id
     * @param fieldName 流程定义ID属性名称
     */
    public static void setWfDefinitionConfigVo(Object obj, List<String> idList, String fieldName) {
        if (CollUtil.isEmpty(idList) || obj == null) {
            return;
        }
        List<WfDefinitionConfigVo> wfDefinitionConfigVoList = I_WF_DEFINITION_CONFIG_SERVICE.queryList(idList);
        if (CollUtil.isNotEmpty(wfDefinitionConfigVoList)) {
            List<Long> formIds = StreamUtils.toList(wfDefinitionConfigVoList, WfDefinitionConfigVo::getFormId);
            List<WfFormManageVo> wfFormManageVos = I_WF_FORM_MANAGE_SERVICE.queryByIds(formIds);
            if (CollUtil.isNotEmpty(wfFormManageVos)) {
                for (WfDefinitionConfigVo wfDefinitionConfigVo : wfDefinitionConfigVoList) {
                    wfFormManageVos.stream().filter(e -> ObjectUtil.equals(wfDefinitionConfigVo.getFormId(), e.getId())).findFirst().ifPresent(wfDefinitionConfigVo::setWfFormManageVo);
                }
            }
        }
        if (obj instanceof Collection<?> collection) {
            for (Object o : collection) {
                String fieldValue = ReflectUtils.invokeGetter(o, fieldName).toString();
                if (!CollUtil.isEmpty(wfDefinitionConfigVoList)) {
                    wfDefinitionConfigVoList.stream().filter(e -> e.getDefinitionId().equals(fieldValue)).findFirst().ifPresent(e -> {
                        ReflectUtils.invokeSetter(o, WF_DEFINITION_CONFIG_VO, e);
                    });
                }
            }
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
    public static void sendMessage(List<Task> list, String name, List<String> messageType, String message) {
        Set<Long> userIds = new HashSet<>();
        if (StringUtils.isBlank(message)) {
            message = "有新的【" + name + "】单据已经提交至您的待办，请您及时处理。";
        }
        for (Task t : list) {
            ParticipantVo taskParticipant = WorkflowUtils.getCurrentTaskParticipant(t.getId());
            if (CollUtil.isNotEmpty(taskParticipant.getGroupIds())) {
                List<SysUserRole> sysUserRoles = WORKFLOW_USER_SERVICE.getUserRoleListByRoleIds(taskParticipant.getGroupIds());
                if (CollUtil.isNotEmpty(sysUserRoles)) {
                    userIds.addAll(StreamUtils.toList(sysUserRoles, SysUserRole::getUserId));
                }
            }
            List<Long> candidate = taskParticipant.getCandidate();
            if (CollUtil.isNotEmpty(candidate)) {
                userIds.addAll(candidate);
            }
        }
        if (CollUtil.isNotEmpty(userIds)) {
            List<SysUserVo> sysUserVoList = WORKFLOW_USER_SERVICE.getUserListByIds(new ArrayList<>(userIds));
            for (String code : messageType) {
                MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByCode(code);
                if (ObjectUtil.isNotEmpty(messageTypeEnum)) {
                    switch (messageTypeEnum) {
                        case SYSTEM_MESSAGE:
                            WebSocketMessageDto dto = new WebSocketMessageDto();
                            dto.setSessionKeys(new ArrayList<>(userIds));
                            dto.setMessage(message);
                            WebSocketUtils.publishMessage(dto);
                            break;
                        case EMAIL_MESSAGE:
                            MailUtils.sendText(StreamUtils.join(sysUserVoList, SysUserVo::getEmail), "单据审批提醒", message);
                            break;
                        case SMS_MESSAGE:
                            //todo 短信发送
                            break;
                    }
                }
            }
        }
    }
}
