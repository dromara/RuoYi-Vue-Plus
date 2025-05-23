package org.dromara.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.dto.UserDTO;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mail.utils.MailUtils;
import org.dromara.common.sse.dto.SseMessageDto;
import org.dromara.common.sse.utils.SseMessageUtils;
import org.dromara.warm.flow.core.constant.ExceptionCons;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.entity.User;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.enums.SkipType;
import org.dromara.warm.flow.core.service.NodeService;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.core.service.UserService;
import org.dromara.warm.flow.core.utils.AssertUtil;
import org.dromara.warm.flow.orm.entity.FlowNode;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.entity.FlowUser;
import org.dromara.warm.flow.orm.mapper.FlowNodeMapper;
import org.dromara.warm.flow.orm.mapper.FlowTaskMapper;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.common.enums.MessageTypeEnum;
import org.dromara.workflow.common.enums.TaskAssigneeType;
import org.dromara.workflow.service.IFlwCommonService;
import org.dromara.workflow.service.IFlwTaskAssigneeService;
import org.dromara.workflow.service.IFlwTaskService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 工作流工具
 *
 * @author LionLi
 */
@ConditionalOnEnable
@Slf4j
@RequiredArgsConstructor
@Service
public class FlwCommonServiceImpl implements IFlwCommonService {

    private final FlowNodeMapper flowNodeMapper;
    private final FlowTaskMapper flowTaskMapper;
    private final UserService userService;
    private final TaskService taskService;
    private final NodeService nodeService;

    /**
     * 获取工作流用户service
     */
    @Override
    public UserService getFlowUserService() {
        return userService;
    }

    /**
     * 构建工作流用户
     *
     * @param userList 办理用户
     * @param taskId   任务ID
     * @return 用户
     */
    @Override
    public Set<User> buildUser(List<User> userList, Long taskId) {
        if (CollUtil.isEmpty(userList)) {
            return Set.of();
        }
        Set<User> list = new HashSet<>();
        Set<String> processedBySet = new HashSet<>();
        IFlwTaskAssigneeService taskAssigneeService = SpringUtils.getBean(IFlwTaskAssigneeService.class);
        for (User user : userList) {
            // 根据 processedBy 前缀判断处理人类型，分别获取用户列表
            List<UserDTO> users = taskAssigneeService.fetchUsersByStorageId(user.getProcessedBy());
            // 转换为 FlowUser 并添加到结果集合
            if (CollUtil.isNotEmpty(users)) {
                users.forEach(dto -> {
                    String processedBy = String.valueOf(dto.getUserId());
                    if (!processedBySet.contains(processedBy)) {
                        FlowUser flowUser = new FlowUser();
                        flowUser.setType(user.getType());
                        flowUser.setProcessedBy(processedBy);
                        flowUser.setAssociated(taskId);
                        list.add(flowUser);
                        processedBySet.add(processedBy);
                    }
                });
            }
        }
        return list;
    }

    /**
     * 构建工作流用户
     *
     * @param userIdList 办理用户
     * @param taskId     任务ID
     * @return 用户
     */
    @Override
    public Set<User> buildFlowUser(List<String> userIdList, Long taskId) {
        if (CollUtil.isEmpty(userIdList)) {
            return Set.of();
        }
        Set<User> list = new HashSet<>();
        Set<String> processedBySet = new HashSet<>();
        for (String userId : userIdList) {
            if (!processedBySet.contains(userId)) {
                FlowUser flowUser = new FlowUser();
                flowUser.setType(TaskAssigneeType.APPROVER.getCode());
                flowUser.setProcessedBy(String.valueOf(userId));
                flowUser.setAssociated(taskId);
                list.add(flowUser);
                processedBySet.add(String.valueOf(userId));
            }
        }
        return list;
    }

    /**
     * 发送消息
     *
     * @param flowName    流程定义名称
     * @param messageType 消息类型
     * @param message     消息内容，为空则发送默认配置的消息内容
     */
    @Override
    public void sendMessage(String flowName, Long instId, List<String> messageType, String message) {
        IFlwTaskService flwTaskService = SpringUtils.getBean(IFlwTaskService.class);
        List<UserDTO> userList = new ArrayList<>();
        List<FlowTask> list = flwTaskService.selectByInstId(instId);
        if (StringUtils.isBlank(message)) {
            message = "有新的【" + flowName + "】单据已经提交至您，请您及时处理。";
        }
        for (Task task : list) {
            List<UserDTO> users = flwTaskService.currentTaskAllUser(task.getId());
            if (CollUtil.isNotEmpty(users)) {
                userList.addAll(users);
            }
        }
        if (CollUtil.isNotEmpty(userList)) {
            for (String code : messageType) {
                MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByCode(code);
                if (ObjectUtil.isNotEmpty(messageTypeEnum)) {
                    switch (messageTypeEnum) {
                        case SYSTEM_MESSAGE:
                            SseMessageDto dto = new SseMessageDto();
                            dto.setUserIds(StreamUtils.toList(userList, UserDTO::getUserId).stream().distinct().collect(Collectors.toList()));
                            dto.setMessage(message);
                            SseMessageUtils.publishMessage(dto);
                            break;
                        case EMAIL_MESSAGE:
                            MailUtils.sendText(StreamUtils.join(userList, UserDTO::getEmail), "单据审批提醒", message);
                            break;
                        case SMS_MESSAGE:
                            //todo 短信发送
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + messageTypeEnum);
                    }
                }
            }
        }
    }

    /**
     * 驳回
     *
     * @param message        审批意见
     * @param instanceId     流程实例id
     * @param targetNodeCode 目标节点
     * @param flowStatus     流程状态
     * @param flowHisStatus  节点操作状态
     */
    @Override
    public void backTask(String message, Long instanceId, String targetNodeCode, String flowStatus, String flowHisStatus) {
        IFlwTaskService flwTaskService = SpringUtils.getBean(IFlwTaskService.class);
        List<FlowTask> list = flwTaskService.selectByInstId(instanceId);
        if (CollUtil.isNotEmpty(list)) {
            List<FlowTask> tasks = StreamUtils.filter(list, e -> e.getNodeCode().equals(targetNodeCode));
            if (list.size() == tasks.size()) {
                return;
            }
        }
        for (FlowTask task : list) {
            List<UserDTO> userList = flwTaskService.currentTaskAllUser(task.getId());
            FlowParams flowParams = FlowParams.build();
            flowParams.nodeCode(targetNodeCode);
            flowParams.message(message);
            flowParams.skipType(SkipType.PASS.getKey());
            flowParams.flowStatus(flowStatus).hisStatus(flowHisStatus);
            flowParams.ignore(true);
            //解决会签没权限问题
            if (CollUtil.isNotEmpty(userList)) {
                flowParams.handler(userList.get(0).getUserId().toString());
            }
            taskService.skip(task.getId(), flowParams);
        }
        //解决会签多人审批问题
        backTask(message, instanceId, targetNodeCode, flowStatus, flowHisStatus);
    }

    /**
     * 申请人节点编码
     *
     * @param definitionId 流程定义id
     * @return 申请人节点编码
     */
    @Override
    public String applyNodeCode(Long definitionId) {
        //获取已发布的流程节点
        List<FlowNode> flowNodes = flowNodeMapper.selectList(new LambdaQueryWrapper<FlowNode>().eq(FlowNode::getDefinitionId, definitionId));
        AssertUtil.isTrue(CollUtil.isEmpty(flowNodes), ExceptionCons.NOT_PUBLISH_NODE);
        Node startNode = flowNodes.stream().filter(t -> NodeType.isStart(t.getNodeType())).findFirst().orElse(null);
        AssertUtil.isNull(startNode, ExceptionCons.LOST_START_NODE);
        Node nextNode = nodeService.getNextNode(definitionId, startNode.getNodeCode(), null, SkipType.PASS.getKey());
        return nextNode.getNodeCode();
    }

    /**
     * 删除运行中的任务
     *
     * @param taskIds 任务id
     */
    @Override
    public void deleteRunTask(List<Long> taskIds) {
        if (CollUtil.isEmpty(taskIds)) {
            return;
        }
        userService.deleteByTaskIds(taskIds);
        flowTaskMapper.deleteByIds(taskIds);
    }
}
