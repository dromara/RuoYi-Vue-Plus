package org.dromara.workflow.event;

import cn.hutool.core.collection.CollUtil;

import java.util.List;

/**
 * 工作流待办消息事件。
 *
 * @param flowName    流程名称
 * @param instanceId  流程实例 ID
 * @param messageType 消息类型
 * @param notice      通知内容
 */
public record WorkflowTaskMessageEvent(String flowName, Long instanceId, List<String> messageType, String notice) {

    public WorkflowTaskMessageEvent {
        if (CollUtil.isNotEmpty(messageType)) {
            messageType = List.copyOf(messageType);
        }
    }

}
