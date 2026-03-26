package org.dromara.common.websocket.utils;

import cn.hutool.core.collection.CollUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.dto.PushPayload;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.websocket.dto.WebSocketMessageDTO;
import org.dromara.common.websocket.holder.WebSocketSessionHolder;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.dromara.common.websocket.constant.WebSocketConstants.WEB_SOCKET_TOPIC;

/**
 * 工具类
 *
 * @author zendwang
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WebSocketUtils {

    /**
     * 向指定会话标识发送文本消息。
     *
     * @param sessionKey 要发送消息的用户id
     * @param message    要发送的消息内容
     */
    public static void sendMessage(Long sessionKey, String message) {
        WebSocketSession session = WebSocketSessionHolder.getSessions(sessionKey);
        sendMessage(session, message);
    }

    /**
     * 向指定会话标识发送统一 JSON 消息。
     *
     * @param sessionKey        要发送消息的用户id
     * @param webSocketMessage  要发送的消息内容
     */
    public static void sendMessage(Long sessionKey, WebSocketMessageDTO webSocketMessage) {
        WebSocketSession session = WebSocketSessionHolder.getSessions(sessionKey);
        sendMessage(session, buildPayload(webSocketMessage));
    }

    /**
     * 订阅 WebSocket 广播主题消息。
     *
     * @param consumer 处理WebSocket消息的消费者函数
     */
    public static void subscribeMessage(Consumer<WebSocketMessageDTO> consumer) {
        RedisUtils.subscribe(WEB_SOCKET_TOPIC, WebSocketMessageDTO.class, consumer);
    }

    /**
     * 按会话标识发布 WebSocket 消息。
     *
     * @param webSocketMessage 要发布的WebSocket消息对象
     */
    public static void publishMessage(WebSocketMessageDTO webSocketMessage) {
        List<Long> unsentSessionKeys = new ArrayList<>();
        // 当前服务内session,直接发送消息
        for (Long sessionKey : webSocketMessage.getSessionKeys()) {
            if (WebSocketSessionHolder.existSession(sessionKey)) {
                WebSocketUtils.sendMessage(sessionKey, webSocketMessage);
                continue;
            }
            unsentSessionKeys.add(sessionKey);
        }
        // 不在当前服务内session,发布订阅消息
        if (CollUtil.isNotEmpty(unsentSessionKeys)) {
            WebSocketMessageDTO broadcastMessage = new WebSocketMessageDTO();
            broadcastMessage.setSessionKeys(unsentSessionKeys);
            broadcastMessage.setMessage(webSocketMessage.getMessage());
            RedisUtils.publish(WEB_SOCKET_TOPIC, broadcastMessage, consumer -> {
                log.info(" WebSocket发送主题订阅消息topic:{} session keys:{} message:{}",
                    WEB_SOCKET_TOPIC, unsentSessionKeys, webSocketMessage.getMessage());
            });
        }
    }

    /**
     * 向所有 WebSocket 会话发布广播消息。
     *
     * @param message 要发布的消息内容
     */
    public static void publishAll(String message) {
        WebSocketMessageDTO broadcastMessage = new WebSocketMessageDTO();
        broadcastMessage.setMessage(message);
        RedisUtils.publish(WEB_SOCKET_TOPIC, broadcastMessage, consumer -> {
            log.info("WebSocket发送主题订阅消息topic:{} message:{}", WEB_SOCKET_TOPIC, message);
        });
    }

    /**
     * 向指定会话标识发布统一 JSON 消息。
     *
     * @param sessionKeys 目标会话
     * @param payload     消息体
     */
    public static void publishMessage(List<Long> sessionKeys, PushPayload payload) {
        WebSocketMessageDTO dto = new WebSocketMessageDTO();
        dto.setSessionKeys(sessionKeys);
        dto.setMessage(payload.getMessage());
        dto.setMessageType(payload.getType());
        dto.setMessageSource(payload.getSource());
        dto.setData(payload.getData());
        dto.setPath(payload.getPath());
        dto.setQuery(payload.getQuery());
        publishMessage(dto);
    }

    /**
     * 向所有 WebSocket 会话发布统一 JSON 消息。
     *
     * @param payload 消息体
     */
    public static void publishAll(PushPayload payload) {
        WebSocketMessageDTO broadcastMessage = new WebSocketMessageDTO();
        broadcastMessage.setMessage(payload.getMessage());
        broadcastMessage.setMessageType(payload.getType());
        broadcastMessage.setMessageSource(payload.getSource());
        broadcastMessage.setData(payload.getData());
        broadcastMessage.setPath(payload.getPath());
        broadcastMessage.setQuery(payload.getQuery());
        RedisUtils.publish(WEB_SOCKET_TOPIC, broadcastMessage, consumer -> {
            log.info("WebSocket发送主题订阅消息topic:{} type:{} source:{} message:{}",
                WEB_SOCKET_TOPIC, payload.getType(), payload.getSource(), payload.getMessage());
        });
    }

    /**
     * 向指定会话发送 Pong 心跳消息。
     *
     * @param session 要发送Pong消息的WebSocket会话
     */
    public static void sendPongMessage(WebSocketSession session) {
        sendMessage(session, new PongMessage());
    }

    /**
     * 向指定 WebSocket 会话发送文本消息。
     *
     * @param session WebSocket会话
     * @param message 要发送的文本消息内容
     */
    public static void sendMessage(WebSocketSession session, String message) {
        sendMessage(session, new TextMessage(message));
    }

    private static String buildPayload(WebSocketMessageDTO webSocketMessage) {
        PushPayload payload = PushPayload.of(
            webSocketMessage.getMessageType(),
            webSocketMessage.getMessageSource(),
            webSocketMessage.getMessage(),
            webSocketMessage.getData()
        );
        payload.setPath(webSocketMessage.getPath());
        payload.setQuery(webSocketMessage.getQuery());
        return JsonUtils.toJsonString(payload);
    }

    /**
     * 向指定 WebSocket 会话发送原始消息对象。
     *
     * @param session WebSocket会话
     * @param message 要发送的WebSocket消息对象
     */
    private static void sendMessage(WebSocketSession session, WebSocketMessage<?> message) {
        if (session == null || !session.isOpen()) {
            log.warn("[send] session会话已经关闭");
        } else {
            try {
                session.sendMessage(message);
            } catch (IOException e) {
                log.error("[send] session({}) 发送消息({}) 异常", session, message, e);
            }
        }
    }
}
