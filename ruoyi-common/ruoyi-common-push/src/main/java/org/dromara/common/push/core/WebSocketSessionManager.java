package org.dromara.common.push.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.dto.PushPayloadDTO;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.push.dto.PushDTO;
import org.dromara.common.redis.utils.RedisUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

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

    private static final Map<Long, Map<String, WebSocketSession>> USER_TOKEN_SESSIONS = new ConcurrentHashMap<>();

    public WebSocketSessionManager() {
        SpringUtils.getBean(ScheduledExecutorService.class)
            .scheduleWithFixedDelay(this::sessionMonitor, 60L, 60L, TimeUnit.SECONDS);
    }

    public void connect(Long userId, String token, WebSocketSession session) {
        Map<String, WebSocketSession> sessions = USER_TOKEN_SESSIONS.computeIfAbsent(userId, key -> new ConcurrentHashMap<>());
        WebSocketSession oldSession = sessions.remove(token);
        closeSession(oldSession, CloseStatus.NORMAL);
        sessions.put(token, session);
    }

    public void disconnect(Long userId, String token) {
        if (userId == null || token == null) {
            return;
        }
        Map<String, WebSocketSession> sessions = USER_TOKEN_SESSIONS.get(userId);
        if (MapUtil.isEmpty(sessions)) {
            USER_TOKEN_SESSIONS.remove(userId);
            return;
        }
        closeSession(sessions.remove(token), CloseStatus.NORMAL);
        if (sessions.isEmpty()) {
            USER_TOKEN_SESSIONS.remove(userId);
        }
    }

    public void sessionMonitor() {
        List<Long> toRemoveUsers = new ArrayList<>();
        USER_TOKEN_SESSIONS.forEach((userId, sessionMap) -> {
            if (CollUtil.isEmpty(sessionMap)) {
                toRemoveUsers.add(userId);
                return;
            }
            sessionMap.entrySet().removeIf(entry -> {
                WebSocketSession session = entry.getValue();
                if (session == null || !session.isOpen()) {
                    closeSession(session, CloseStatus.NORMAL);
                    return true;
                }
                return false;
            });
            if (sessionMap.isEmpty()) {
                toRemoveUsers.add(userId);
            }
        });
        toRemoveUsers.forEach(USER_TOKEN_SESSIONS::remove);
    }

    @Override
    public void subscribeMessage(Consumer<PushDTO> consumer) {
        RedisUtils.subscribe(MESSAGE_TOPIC, PushDTO.class, consumer);
    }

    @Override
    public void sendMessage(Long userId, PushPayloadDTO payload) {
        if (payload == null) {
            return;
        }
        Map<String, WebSocketSession> sessions = USER_TOKEN_SESSIONS.get(userId);
        if (MapUtil.isEmpty(sessions)) {
            USER_TOKEN_SESSIONS.remove(userId);
            return;
        }
        sessions.entrySet().removeIf(entry -> {
            WebSocketSession session = entry.getValue();
            if (session == null || !session.isOpen()) {
                closeSession(session, CloseStatus.NORMAL);
                return true;
            }
            return !sendMessage(session, new TextMessage(JsonUtils.toJsonString(payload)));
        });
        if (sessions.isEmpty()) {
            USER_TOKEN_SESSIONS.remove(userId);
        }
    }

    @Override
    public void sendMessage(PushPayloadDTO payload) {
        USER_TOKEN_SESSIONS.keySet().forEach(userId -> sendMessage(userId, payload));
    }

    @Override
    public void publishMessage(PushDTO pushDTO) {
        RedisUtils.publish(MESSAGE_TOPIC, pushDTO, consumer -> log.info(
            "WebSocket发送主题订阅消息topic:{} userIds:{} message:{}",
            MESSAGE_TOPIC,
            pushDTO.getUserIds(),
            pushDTO.getPayload() == null ? null : pushDTO.getPayload().getMessage()
        ));
    }

    @Override
    public void publishAll(PushPayloadDTO payload) {
        PushDTO dto = new PushDTO();
        dto.setPayload(payload);
        publishMessage(dto);
    }

    public void sendPongMessage(WebSocketSession session) {
        sendMessage(session, new PongMessage());
    }

    public void sendMessage(WebSocketSession session, String message) {
        sendMessage(session, new TextMessage(message));
    }

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

    private void closeSession(WebSocketSession session, CloseStatus status) {
        if (session == null) {
            return;
        }
        try {
            session.close(status);
        } catch (Exception ignored) {
        }
    }
}
