package org.dromara.workflow.event;

import cn.hutool.core.collection.CollUtil;

import java.util.List;

/**
 * 工作流结果消息事件。
 *
 * @param flowName    流程名称
 * @param status      流程状态
 * @param createBy    发起人用户 ID
 * @param messageType 消息类型
 */
public record WorkflowResultMessageEvent(String flowName, String status, String createBy, List<String> messageType) {

    public WorkflowResultMessageEvent {
        if (CollUtil.isNotEmpty(messageType)) {
            messageType = List.copyOf(messageType);
        }
    }

}
