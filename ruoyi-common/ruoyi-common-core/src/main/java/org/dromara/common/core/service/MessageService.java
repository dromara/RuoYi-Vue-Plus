package org.dromara.common.core.service;

import org.dromara.common.core.domain.dto.PushPayloadDTO;

import java.util.List;

/**
 * 通用 消息服务
 *
 * @author Lion Li
 */
public interface MessageService {

    void sendMessage(Long userId, String message);

    void sendMessage(String message);

    void sendMessage(Long userId, PushPayloadDTO payload);

    void sendMessage(PushPayloadDTO payload);

    void publishMessage(List<Long> userIds, PushPayloadDTO payload);

    void publishAll(String message);

    void publishAll(PushPayloadDTO payload);
}
