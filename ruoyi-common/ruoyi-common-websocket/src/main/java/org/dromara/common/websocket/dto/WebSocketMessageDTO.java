package org.dromara.common.websocket.dto;

import java.util.List;

/**
 * 消息的DTO
 *
 * @param sessionKeys WebSocket 会话标识列表
 * @param message     推送消息内容
 * @author zendwang
 */
public record WebSocketMessageDTO(
    List<Long> sessionKeys,
    String message
) {
}
