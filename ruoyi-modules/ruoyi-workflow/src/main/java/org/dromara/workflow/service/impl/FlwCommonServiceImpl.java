package org.dromara.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.dto.UserDTO;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mail.utils.MailUtils;
import org.dromara.common.sse.dto.SseMessageDto;
import org.dromara.common.sse.utils.SseMessageUtils;
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
        sendMessage(messageType, message, DEFAULT_SUBJECT, userList);
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
        if (CollUtil.isEmpty(messageType) || CollUtil.isEmpty(userList)) {
            return;
        }
        List<Long> userIds = new ArrayList<>(StreamUtils.toSet(userList, UserDTO::getUserId));
        Set<String> emails = StreamUtils.toSet(userList, UserDTO::getEmail);

        for (String code : messageType) {
            MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByCode(code);
            if (ObjectUtil.isEmpty(messageTypeEnum)) {
                continue;
            }
            try {
                switch (messageTypeEnum) {
                    case SYSTEM_MESSAGE -> {
                        SseMessageDto dto = new SseMessageDto();
                        dto.setUserIds(userIds);
                        dto.setMessage(message);
                        SseMessageUtils.publishMessage(dto);
                    }
                    case EMAIL_MESSAGE -> MailUtils.sendText(emails, subject, message);
                    case SMS_MESSAGE -> {
//                        LinkedHashMap<String, String> map = new LinkedHashMap<>(1);
//                        // 根据具体短信服务商参数用法传参
//                        map.put("code", "1234");
//                        // 自动获取一个短信服务商
//                        SmsBlend smsBlend = SmsFactory.getSmsBlend();
//                        // 指定获取一个短信服务商 configKey
//                        SmsBlend smsBlend = SmsFactory.getSmsBlend("config1");
//                        SmsResponse smsResponse = smsBlend.sendMessage(phones, templateId, map);
                        log.info("【短信发送 - TODO】用户数量={} 内容={}", userList.size(), message);
                    }
                    default -> log.warn("【消息发送】未处理的消息类型：{}", messageTypeEnum);
                }
            } catch (Exception ex) {
                // 记录错误但不抛出，确保主逻辑不受影响
                log.error("【消息发送失败】类型={}，原因={}", messageTypeEnum, ex.getMessage(), ex);
            }
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
