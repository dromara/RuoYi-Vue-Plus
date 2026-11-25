package org.dromara.common.push.core;

import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.ThreadUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.push.constant.MessageConstants;
import org.dromara.common.push.dto.PushDTO;
import org.dromara.common.push.properties.MessageProperties;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.system.api.domain.PushPayloadDTO;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.WebSocketSessionDecorator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.dromara.common.push.constant.MessageConstants.MESSAGE_TOPIC;

/**
 * WebSocket 会话管理器。
 *
 * @author Lion Li
 */
@Slf4j
public class WebSocketSessionManager implements PushSessionManager {

    /**
     * 用户会话存储集合
     * 结构：userId -> (token -> WebSocketSession)
     * 支持同一用户多终端、多设备同时在线
     */
    private static final Map<Long, Map<String, WebSocketSession>> USER_TOKEN_SESSIONS = new ConcurrentHashMap<>();

    /**
     * 构造函数
     * 初始化定时任务：每60秒执行一次会话监控，自动清理无效连接
     */
    public WebSocketSessionManager(ScheduledExecutorService scheduledExecutorService, MessageProperties messageProperties) {
        scheduledExecutorService.scheduleWithFixedDelay(
            this::sessionMonitor,
            messageProperties.getHeartbeatInterval(),
            messageProperties.getHeartbeatInterval(),
            TimeUnit.SECONDS
        );
    }

    /**
     * 用户建立WebSocket连接
     *
     * @param userId  用户ID
     * @param token   客户端唯一标识（区分不同设备/终端）
     * @param session WebSocket会话对象
     */
    public void connect(Long userId, String token, WebSocketSession session) {
        WebSocketSession oldSession;
        synchronized (USER_TOKEN_SESSIONS) {
            oldSession = USER_TOKEN_SESSIONS.computeIfAbsent(userId, key -> new ConcurrentHashMap<>()).put(token, session);
        }
        sendKickedMessage(oldSession);
        closeSession(oldSession, CloseStatus.NORMAL);
    }

    /**
     * 用户断开WebSocket连接
     *
     * @param userId 用户ID
     * @param token  客户端唯一标识
     */
    public void disconnect(Long userId, String token) {
        disconnect(userId, token, CloseStatus.NORMAL);
    }

    /**
     * 用户断开WebSocket连接
     *
     * @param userId 用户ID
     * @param token  客户端唯一标识
     * @param status 关闭状态码
     */
    public void disconnect(Long userId, String token, CloseStatus status) {
        disconnect(userId, token, null, status);
    }

    /** 按实际连接注销，避免迟到的回调移除同 token 的新连接。 */
    public void disconnect(Long userId, String token, WebSocketSession expected, CloseStatus status) {
        if (userId == null || token == null) {
            return;
        }
        WebSocketSession current;
        synchronized (USER_TOKEN_SESSIONS) {
            Map<String, WebSocketSession> sessions = USER_TOKEN_SESSIONS.get(userId);
            current = sessions == null ? null : sessions.get(token);
            if (current == null || (expected != null
                && WebSocketSessionDecorator.unwrap(current) != WebSocketSessionDecorator.unwrap(expected))) {
                return;
            }
            sessions.remove(token);
            if (sessions.isEmpty()) {
                USER_TOKEN_SESSIONS.remove(userId);
            }
        }
        closeSession(current, status);
    }

    /**
     * 会话监控定时任务
     * 定期清理已关闭、失效的WebSocket会话，防止内存泄漏
     */
    public void sessionMonitor() {
        USER_TOKEN_SESSIONS.forEach((userId, sessions) ->
            sessions.forEach((token, session) -> {
                if (!session.isOpen()) {
                    disconnect(userId, token, session, CloseStatus.NORMAL);
                }
            }));
    }

    /**
     * 通知旧连接已被同 token 新连接替换。
     *
     * @param session 旧 WebSocket 会话
     */
    private void sendKickedMessage(WebSocketSession session) {
        if (session == null || !session.isOpen()) {
            return;
        }
        sendMessage(session, MessageConstants.KICKED);
    }

    /**
     * 订阅消息通道
     * 注册消息消费者，监听Redis消息推送
     *
     * @param consumer 消息消费逻辑
     */
    @Override
    public void subscribeMessage(Consumer<PushDTO> consumer) {
        RedisUtils.subscribe(MESSAGE_TOPIC, PushDTO.class, consumer);
    }

    /**
     * 向指定用户发送消息
     *
     * @param userId  目标用户ID
     * @param payload 消息体
     */
    @Override
    public void sendMessage(Long userId, PushPayloadDTO payload) {
        if (payload == null) {
            return;
        }
        Map<String, WebSocketSession> sessions = USER_TOKEN_SESSIONS.get(userId);
        if (MapUtil.isEmpty(sessions)) {
            return;
        }
        // 发送消息并自动清理失效会话
        sessions.forEach((token, session) -> {
            if (!session.isOpen()) {
                disconnect(userId, token, session, CloseStatus.NORMAL);
            } else if (!sendMessage(session, new TextMessage(JsonUtils.toJsonString(payload)))) {
                disconnect(userId, token, session, CloseStatus.SERVER_ERROR);
            }
        });
    }

    /**
     * 向所有在线用户广播消息
     *
     * @param payload 消息体
     */
    @Override
    public void sendMessage(PushPayloadDTO payload) {
        if (payload == null) {
            return;
        }
        List<Long> userIds = new ArrayList<>(USER_TOKEN_SESSIONS.keySet());
        Runnable[] sendTasks = userIds.stream()
            .map(userId -> (Runnable) () -> sendMessage(userId, payload))
            .toArray(Runnable[]::new);
        ThreadUtils.virtualInvokeAll(sendTasks);
    }

    /**
     * 发布消息到Redis订阅通道
     * 支持集群环境下的分布式消息推送
     *
     * @param pushDTO 推送消息封装对象
     */
    @Override
    public void publishMessage(PushDTO pushDTO) {
        if (pushDTO == null || pushDTO.getPayload() == null) {
            return;
        }
        RedisUtils.publish(MESSAGE_TOPIC, pushDTO, consumer -> log.info(
            "WebSocket发送主题订阅消息topic:{} userIds:{} message:{}",
            MESSAGE_TOPIC,
            pushDTO.getUserIds(),
            pushDTO.getPayload() == null ? null : pushDTO.getPayload().getMessage()
        ));
    }

    /**
     * 全局广播消息（所有用户）
     *
     * @param payload 消息体
     */
    @Override
    public void publishAll(PushPayloadDTO payload) {
        publishMessage(PushDTO.broadcast(payload));
    }

    /**
     * 发送心跳Pong消息
     * 用于维持WebSocket长连接存活
     *
     * @param session WebSocket会话
     */
    public void sendPongMessage(WebSocketSession session) {
        sendMessage(session, new PongMessage());
    }

    /**
     * 发送文本消息
     *
     * @param session WebSocket会话
     * @param message 文本内容
     */
    public void sendMessage(WebSocketSession session, String message) {
        sendMessage(session, new TextMessage(message));
    }

    /**
     * 底层消息发送方法
     *
     * @param session 会话对象
     * @param message WebSocket消息对象
     * @return 发送是否成功
     */
    private boolean sendMessage(WebSocketSession session, WebSocketMessage<?> message) {
        if (session == null || !session.isOpen()) {
            log.warn("[send] session会话已经关闭");
            return false;
        }
        try {
            session.sendMessage(message);
            return true;
        } catch (IOException e) {
            log.error("[send] session({}) 发送消息({}) 异常", session, message, e);
            return false;
        }
    }

    /**
     * 安全关闭WebSocket会话
     *
     * @param session 待关闭的会话
     * @param status  关闭状态码
     */
    public void closeSession(WebSocketSession session, CloseStatus status) {
        if (session == null) {
            return;
        }
        try {
            session.close(status);
        } catch (Exception ignored) {
            // 关闭异常忽略，防止影响主流程
        }
    }
}
