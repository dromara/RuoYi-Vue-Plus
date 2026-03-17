package org.dromara.common.sse.dto;

import java.util.List;

/**
 * 消息的DTO
 *
 * @param userIds 接收消息的用户 ID 列表
 * @param message 推送消息内容
 * @author zendwang
 */
public record SseMessageDTO(
    List<Long> userIds,
    String message
) {
}
