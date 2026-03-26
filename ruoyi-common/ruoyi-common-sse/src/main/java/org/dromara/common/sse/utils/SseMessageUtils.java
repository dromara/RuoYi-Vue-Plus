package org.dromara.common.sse.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.dto.PushPayload;
import org.dromara.common.core.enums.PushSourceEnum;
import org.dromara.common.core.enums.PushTypeEnum;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.sse.core.SseEmitterManager;
import org.dromara.common.sse.dto.SseMessageDTO;

import java.util.List;

/**
 * SSE工具类
 *
 * @author Lion Li
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SseMessageUtils {

    private final static Boolean SSE_ENABLE = SpringUtils.getProperty("sse.enabled", Boolean.class, true);
    private static SseEmitterManager MANAGER;

    static {
        if (isEnable() && MANAGER == null) {
            MANAGER = SpringUtils.getBean(SseEmitterManager.class);
        }
    }

    /**
     * 向指定用户的 SSE 会话发送消息。
     *
     * @param userId  要发送消息的用户id
     * @param message 要发送的消息内容
     */
    public static void sendMessage(Long userId, String message) {
        if (!isEnable()) {
            return;
        }
        MANAGER.sendMessage(userId, buildMessage(message));
    }

    /**
     * 向当前节点上的所有 SSE 会话发送消息。
     *
     * @param message 要发送的消息内容
     */
    public static void sendMessage(String message) {
        if (!isEnable()) {
            return;
        }
        MANAGER.sendMessage(buildMessage(message));
    }

    /**
     * 向指定用户的 SSE 会话发送统一 JSON 消息。
     *
     * @param userId  要发送消息的用户id
     * @param payload 要发送的消息体
     */
    public static void sendMessage(Long userId, PushPayload payload) {
        if (!isEnable()) {
            return;
        }
        MANAGER.sendMessage(userId, payload);
    }

    /**
     * 向当前节点上的所有 SSE 会话发送统一 JSON 消息。
     *
     * @param payload 要发送的消息体
     */
    public static void sendMessage(PushPayload payload) {
        if (!isEnable()) {
            return;
        }
        MANAGER.sendMessage(payload);
    }

    /**
     * 发布 SSE 订阅消息。
     *
     * @param sseMessageDTO 要发布的SSE消息对象
     */
    public static void publishMessage(SseMessageDTO sseMessageDTO) {
        if (!isEnable()) {
            return;
        }
        MANAGER.publishMessage(sseMessageDTO);
    }

    /**
     * 向所有用户发布 SSE 广播消息。
     *
     * @param message 要发布的消息内容
     */
    public static void publishAll(String message) {
        if (!isEnable()) {
            return;
        }
        MANAGER.publishAll(buildMessage(message));
    }

    /**
     * 向指定用户发布统一 JSON 消息。
     *
     * @param userIds  目标用户
     * @param payload  消息体
     */
    public static void publishMessage(List<Long> userIds, PushPayload payload) {
        if (!isEnable()) {
            return;
        }
        SseMessageDTO dto = new SseMessageDTO();
        dto.setUserIds(userIds);
        dto.setMessage(payload.getMessage());
        dto.setMessageType(payload.getType());
        dto.setMessageSource(payload.getSource());
        dto.setData(payload.getData());
        dto.setPath(payload.getPath());
        dto.setQuery(payload.getQuery());
        MANAGER.publishMessage(dto);
    }

    /**
     * 向所有用户发布统一 JSON 消息。
     *
     * @param payload 消息体
     */
    public static void publishAll(PushPayload payload) {
        if (!isEnable()) {
            return;
        }
        MANAGER.publishAll(payload);
    }

    /**
     * 判断 SSE 功能是否启用。
     *
     * @return 是否启用
     */
    public static Boolean isEnable() {
        return SSE_ENABLE;
    }

    private static PushPayload buildMessage(String message) {
        return PushPayload.of(PushTypeEnum.MESSAGE, PushSourceEnum.BACKEND, message, null);
    }

}
