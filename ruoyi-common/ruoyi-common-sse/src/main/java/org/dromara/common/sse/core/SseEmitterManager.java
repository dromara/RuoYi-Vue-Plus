package org.dromara.common.sse.core;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.sse.dto.SseMessageDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
public class SseEmitterManager {
    /**
     * 订阅的频道
     */
    private final static String SSE_TOPIC = "global:sse";

    private final static Map<Long, Map<String, SseEmitter>> USER_TOKEN_EMITTERS = new ConcurrentHashMap<>();

    public SseEmitter connect(Long userId, String token) {
        Map<String, SseEmitter> emitters = USER_TOKEN_EMITTERS.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
        SseEmitter emitter = new SseEmitter(0L);

        emitters.put(token, emitter);

        emitter.onCompletion(() -> emitters.remove(token));
        emitter.onTimeout(() -> emitters.remove(token));
        emitter.onError((e) -> emitters.remove(token));

        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            emitters.remove(token);
        }
        return emitter;
    }

    public void disconnect(Long userId, String token) {
        Map<String, SseEmitter> emitters = USER_TOKEN_EMITTERS.get(userId);
        if (emitters != null) {
            try {
                emitters.get(token).send(SseEmitter.event().comment("disconnected"));
            } catch (Exception ignore) {
            }
            emitters.remove(token);
        }
    }

    /**
     * 订阅SSE消息主题，并提供一个消费者函数来处理接收到的消息
     *
     * @param consumer 处理SSE消息的消费者函数
     */
    public void subscribeMessage(Consumer<SseMessageDto> consumer) {
        RedisUtils.subscribe(SSE_TOPIC, SseMessageDto.class, consumer);
    }

    /**
     * 向指定的用户会话发送消息
     *
     * @param userId  要发送消息的用户id
     * @param message 要发送的消息内容
     */
    public void sendMessage(Long userId, String message) {
        Map<String, SseEmitter> emitters = USER_TOKEN_EMITTERS.get(userId);
        if (emitters != null) {
            for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
                try {
                    // token 无效或已过期
                    if (StpUtil.stpLogic.getTokenActiveTimeoutByToken(entry.getKey()) < -1) {
                        emitters.remove(entry.getKey());
                        continue;
                    }
                    entry.getValue().send(SseEmitter.event()
                        .name("message")
                        .data(message));
                } catch (Exception e) {
                    emitters.remove(entry.getKey());
                }
            }
        }
    }

    /**
     * 本机全用户会话发送消息
     *
     * @param message 要发送的消息内容
     */
    public void sendMessage(String message) {
        for (Long userId : USER_TOKEN_EMITTERS.keySet()) {
            sendMessage(userId, message);
        }
    }

    /**
     * 发布SSE订阅消息
     *
     * @param sseMessageDto 要发布的SSE消息对象
     */
    public void publishMessage(SseMessageDto sseMessageDto) {
        List<Long> unsentUserIds = new ArrayList<>();
        // 当前服务内用户,直接发送消息
        for (Long userId : sseMessageDto.getUserIds()) {
            if (USER_TOKEN_EMITTERS.containsKey(userId)) {
                sendMessage(userId, sseMessageDto.getMessage());
                continue;
            }
            unsentUserIds.add(userId);
        }
        // 不在当前服务内用户,发布订阅消息
        if (CollUtil.isNotEmpty(unsentUserIds)) {
            SseMessageDto broadcastMessage = new SseMessageDto();
            broadcastMessage.setMessage(sseMessageDto.getMessage());
            broadcastMessage.setUserIds(unsentUserIds);
            RedisUtils.publish(SSE_TOPIC, broadcastMessage, consumer -> {
                log.info("SSE发送主题订阅消息topic:{} session keys:{} message:{}",
                    SSE_TOPIC, unsentUserIds, sseMessageDto.getMessage());
            });
        }
    }

    /**
     * 向所有的用户发布订阅的消息(群发)
     *
     * @param message 要发布的消息内容
     */
    public void publishAll(String message) {
        SseMessageDto broadcastMessage = new SseMessageDto();
        broadcastMessage.setMessage(message);
        RedisUtils.publish(SSE_TOPIC, broadcastMessage, consumer -> {
            log.info("SSE发送主题订阅消息topic:{} message:{}", SSE_TOPIC, message);
        });
    }
}
