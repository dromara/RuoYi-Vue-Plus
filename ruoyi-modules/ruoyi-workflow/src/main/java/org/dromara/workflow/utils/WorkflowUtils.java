package org.dromara.workflow.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.domain.dto.RoleDTO;
import org.dromara.common.core.domain.dto.UserDTO;
import org.dromara.common.core.service.UserService;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mail.utils.MailUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.common.websocket.dto.WebSocketMessageDto;
import org.dromara.common.websocket.utils.WebSocketUtils;
import org.dromara.workflow.common.constant.FlowConstant;
import org.dromara.workflow.common.enums.MessageTypeEnum;
import org.dromara.workflow.common.enums.TaskStatusEnum;
import org.dromara.workflow.domain.ActHiTaskinst;
import org.dromara.workflow.domain.vo.MultiInstanceVo;
import org.dromara.workflow.domain.vo.ParticipantVo;
import org.dromara.workflow.flowable.cmd.UpdateHiTaskInstCmd;
import org.dromara.workflow.mapper.ActHiTaskinstMapper;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;
import org.flowable.engine.impl.bpmn.behavior.SequentialMultiInstanceBehavior;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

import java.util.*;

/**
 * 工作流工具
 *
 * @author may
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkflowUtils {

    private static final ProcessEngine PROCESS_ENGINE = SpringUtils.getBean(ProcessEngine.class);
    private static final ActHiTaskinstMapper ACT_HI_TASKINST_MAPPER = SpringUtils.getBean(ActHiTaskinstMapper.class);

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
    public static ParticipantVo getCurrentTaskParticipant(String taskId, UserService userService) {
        ParticipantVo participantVo = new ParticipantVo();
        List<HistoricIdentityLink> linksForTask = PROCESS_ENGINE.getHistoryService().getHistoricIdentityLinksForTask(taskId);
        Task task = QueryUtils.taskQuery().taskId(taskId).singleResult();
        if (task != null && CollUtil.isNotEmpty(linksForTask)) {
            List<HistoricIdentityLink> groupList = StreamUtils.filter(linksForTask, e -> StringUtils.isNotBlank(e.getGroupId()));
            if (CollUtil.isNotEmpty(groupList)) {
                List<Long> groupIds = StreamUtils.toList(groupList, e -> Long.valueOf(e.getGroupId()));
                List<Long> userIds = userService.selectUserIdsByRoleIds(groupIds);
                if (CollUtil.isNotEmpty(userIds)) {
                    participantVo.setGroupIds(groupIds);
                    List<UserDTO> userList = userService.selectListByIds(userIds);
                    if (CollUtil.isNotEmpty(userList)) {
                        List<Long> userIdList = StreamUtils.toList(userList, UserDTO::getUserId);
                        List<String> nickNames = StreamUtils.toList(userList, UserDTO::getNickName);
                        participantVo.setCandidate(userIdList);
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
                List<UserDTO> userList = userService.selectListByIds(userIdList);
                if (CollUtil.isNotEmpty(userList)) {
                    List<Long> userIds = StreamUtils.toList(userList, UserDTO::getUserId);
                    List<String> nickNames = StreamUtils.toList(userList, UserDTO::getNickName);
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
     * @param businessKey 业务id
     */
    public static String getBusinessStatus(String businessKey) {
        HistoricProcessInstance historicProcessInstance = QueryUtils.hisBusinessKeyQuery(businessKey).singleResult();
        return historicProcessInstance.getBusinessStatus();
    }

    /**
     * 发送消息
     *
     * @param list        任务
     * @param name        流程名称
     * @param messageType 消息类型
     * @param message     消息内容，为空则发送默认配置的消息内容
     */
    public static void sendMessage(List<Task> list, String name, List<String> messageType, String message, UserService userService) {
        Set<Long> userIds = new HashSet<>();
        if (StringUtils.isBlank(message)) {
            message = "有新的【" + name + "】单据已经提交至您的待办，请您及时处理。";
        }
        for (Task t : list) {
            ParticipantVo taskParticipant = WorkflowUtils.getCurrentTaskParticipant(t.getId(), userService);
            if (CollUtil.isNotEmpty(taskParticipant.getGroupIds())) {
                List<Long> userIdList = userService.selectUserIdsByRoleIds(taskParticipant.getGroupIds());
                if (CollUtil.isNotEmpty(userIdList)) {
                    userIds.addAll(userIdList);
                }
            }
            List<Long> candidate = taskParticipant.getCandidate();
            if (CollUtil.isNotEmpty(candidate)) {
                userIds.addAll(candidate);
            }
        }
        if (CollUtil.isNotEmpty(userIds)) {
            List<UserDTO> userList = userService.selectListByIds(new ArrayList<>(userIds));
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
                            MailUtils.sendText(StreamUtils.join(userList, UserDTO::getEmail), "单据审批提醒", message);
                            break;
                        case SMS_MESSAGE:
                            //todo 短信发送
                            break;
                    }
                }
            }
        }
    }

    /**
     * 根据任务id查询 当前用户的任务，检查 当前人员 是否是该 taskId 的办理人
     *
     * @param taskId 任务id
     * @return 结果
     */
    public static Task getTaskByCurrentUser(String taskId) {
        TaskQuery taskQuery = QueryUtils.taskQuery();
        taskQuery.taskId(taskId).taskCandidateOrAssigned(String.valueOf(LoginHelper.getUserId()));

        List<RoleDTO> roles = LoginHelper.getLoginUser().getRoles();
        if (CollUtil.isNotEmpty(roles)) {
            List<String> groupIds = StreamUtils.toList(roles, e -> String.valueOf(e.getRoleId()));
            taskQuery.taskCandidateGroupIn(groupIds);
        }
        return taskQuery.singleResult();
    }
}
