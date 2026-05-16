package org.dromara.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.enums.PushSourceEnum;
import org.dromara.common.core.enums.PushTypeEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.ThreadUtils;
import org.dromara.common.mail.core.MailBuilder;
import org.dromara.system.api.MessageService;
import org.dromara.system.api.domain.PushPayloadDTO;
import org.dromara.system.api.domain.UserDTO;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.common.enums.MessageTypeEnum;
import org.dromara.workflow.service.IFlwCommonService;
import org.dromara.workflow.service.IFlwTaskService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.dromara.workflow.common.constant.FlowConstant.PATH_MY_DOCUMENT;
import static org.dromara.workflow.common.constant.FlowConstant.PATH_TASK_WAITING;

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

    private static final String DEFAULT_SUBJECT = "单据审批提醒";
    private final MessageService messageService;

    /**
     * 根据流程实例发送消息给当前处理人
     *
     * @param flowName    流程定义名称
     * @param instId      流程实例ID
     * @param messageType 消息类型列表
     * @param message     消息内容，为空则使用默认消息
     */
    @Override
    public void sendMessage(String flowName, Long instId, List<String> messageType, String message) {
        if (CollUtil.isEmpty(messageType)) {
            return;
        }
        IFlwTaskService flwTaskService = SpringUtils.getBean(IFlwTaskService.class);
        List<FlowTask> list = flwTaskService.selectByInstId(instId);
        if (CollUtil.isEmpty(list)) {
            return;
        }
        if (StringUtils.isBlank(message)) {
            message = "有新的【" + flowName + "】单据已经提交至您，请您及时处理。";
        }
        List<UserDTO> userList = flwTaskService.currentTaskAllUser(StreamUtils.toList(list, FlowTask::getId));
        if (CollUtil.isEmpty(userList)) {
            return;
        }
        // 发给当前处理人的工作流消息统一进入“我的待办”。
        sendMessage(messageType, message, DEFAULT_SUBJECT, userList, PATH_TASK_WAITING);
    }

    /**
     * 发送消息给指定用户列表
     *
     * @param messageType 消息类型列表
     * @param message     消息内容
     * @param subject     邮件标题
     * @param userList    接收用户列表
     */
    @Override
    public void sendMessage(List<String> messageType, String message, String subject, List<UserDTO> userList) {
        sendMessage(messageType, message, subject, userList, null);
    }

    @Override
    public void sendResultMessage(String flowName, BusinessStatusEnum status, List<String> messageType, List<UserDTO> userList) {
        if (status == null || CollUtil.isEmpty(messageType) || CollUtil.isEmpty(userList)) {
            return;
        }
        // 审批结果类消息面向发起人查看，统一跳转到“我发起的”。
        String message = "您发起的【" + flowName + "】单据审批已" + status.getDesc() + "。";
        sendMessage(messageType, message, DEFAULT_SUBJECT, userList, PATH_MY_DOCUMENT);
    }

    @Override
    public void sendMessage(List<String> messageType, String message, String subject, List<UserDTO> userList, String path) {
        if (CollUtil.isEmpty(messageType) || CollUtil.isEmpty(userList)) {
            return;
        }
        List<Long> userIds = new ArrayList<>(StreamUtils.toSet(userList, UserDTO::getUserId));
        Set<String> emails = StreamUtils.toSet(userList, UserDTO::getEmail);
        emails.removeIf(StringUtils::isBlank);

        Runnable[] sendTasks = messageType.stream()
            .map(code -> (Runnable) () -> sendMessageByType(code, message, subject, path, userIds, emails, userList.size()))
            .toArray(Runnable[]::new);
        ThreadUtils.virtualInvokeAll(sendTasks);
    }

    private void sendMessageByType(String code, String message, String subject, String path, List<Long> userIds, Set<String> emails, int userCount) {
        MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByCode(code);
        if (ObjectUtil.isEmpty(messageTypeEnum)) {
            return;
        }
        try {
            switch (messageTypeEnum) {
                case SYSTEM_MESSAGE -> {
                    // 站内消息直接携带前端路由，消息盒子点击后可按路径分流。
                    messageService.publishMessage(userIds, PushPayloadDTO.of(
                        PushTypeEnum.MESSAGE,
                        PushSourceEnum.WORKFLOW,
                        message, null, path
                    ));
                }
                case EMAIL_MESSAGE -> MailBuilder.of().to(emails).subject(subject).text(message).send();
                case SMS_MESSAGE -> {
//                        LinkedHashMap<String, String> map = new LinkedHashMap<>(1);
//                        // 根据具体短信服务商参数用法传参
//                        map.put("code", "1234");
//                        // 自动获取一个短信服务商
//                        SmsBlend smsBlend = SmsFactory.getSmsBlend();
//                        // 指定获取一个短信服务商 configKey
//                        SmsBlend smsBlend = SmsFactory.getSmsBlend("config1");
//                        SmsResponse smsResponse = smsBlend.sendMessage(phones, templateId, map);
                    log.info("【短信发送 - TODO】用户数量={} 内容={}", userCount, message);
                }
                default -> log.warn("【消息发送】未处理的消息类型：{}", messageTypeEnum);
            }
        } catch (Exception ex) {
            // 记录错误但不抛出，确保主逻辑不受影响
            log.error("【消息发送失败】类型={}，原因={}", messageTypeEnum, ex.getMessage(), ex);
        }
    }

    /**
     * 申请人节点编码
     *
     * @param definitionId 流程定义id
     * @return 申请人节点编码
     */
    @Override
    public String applyNodeCode(Long definitionId) {
        List<Node> firstBetweenNode = FlowEngine.nodeService().getFirstBetweenNode(definitionId, new HashMap<>());
        if (CollUtil.isEmpty(firstBetweenNode)) {
            throw new ServiceException("流程定义缺少申请人节点，请检查流程定义配置");
        }
        return firstBetweenNode.get(0).getNodeCode();
    }
}
