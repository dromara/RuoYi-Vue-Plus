package org.dromara.common.push;

import org.dromara.common.push.annotation.ConditionalOnMessageTransport;
import org.dromara.common.push.condition.MessageTransportCondition;
import org.dromara.common.push.enums.MessageTransportEnum;
import org.dromara.common.push.core.SseEmitterSessionManager;
import org.dromara.common.push.properties.MessageProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("common-push 功能单元测试")
class PushFunctionTest {

    /**
     * 验证消息传输枚举忽略大小写解析，并在未知配置下回退到 SSE。
     */
    @Test
    @DisplayName("解析消息传输方式")
    void shouldResolveMessageTransportWithSseFallback() {
        assertEquals(MessageTransportEnum.WEBSOCKET, MessageTransportEnum.of("WebSocket"));
        assertEquals(MessageTransportEnum.SSE, MessageTransportEnum.of("unknown"));
        assertEquals(MessageTransportEnum.SSE, MessageTransportEnum.of(null));
    }

    /**
     * 验证传输条件同时受启用开关和传输类型约束，防止错误装配推送实现。
     */
    @Test
    @DisplayName("匹配消息传输装配条件")
    void shouldMatchEnabledMessageTransportOnly() {
        MessageTransportCondition condition = new MessageTransportCondition();
        ConditionContext context = mock(ConditionContext.class);
        AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
        MockEnvironment environment = new MockEnvironment()
            .withProperty("message.enabled", "true")
            .withProperty("message.transport", "websocket");
        when(context.getEnvironment()).thenReturn(environment);
        when(metadata.getAnnotationAttributes(ConditionalOnMessageTransport.class.getName()))
            .thenReturn(Map.of("value", "websocket"));

        assertTrue(condition.matches(context, metadata));

        environment.setProperty("message.enabled", "false");
        assertFalse(condition.matches(context, metadata));
    }

    @Test
    @DisplayName("旧 SSE 回调不移除重连后的新连接")
    @SuppressWarnings("unchecked")
    void shouldKeepReplacementEmitterWhenOldConnectionCompletes() {
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        SseEmitterSessionManager manager = new SseEmitterSessionManager(scheduler, new MessageProperties());
        Map<Long, Map<String, SseEmitter>> sessions =
            (Map<Long, Map<String, SseEmitter>>) ReflectionTestUtils.getField(SseEmitterSessionManager.class, "USER_TOKEN_EMITTERS");
        long userId = 999_999L;
        String token = "replacement-test";
        SseEmitter oldEmitter = new SseEmitter();
        SseEmitter newEmitter = new SseEmitter();
        sessions.put(userId, new ConcurrentHashMap<>(Map.of(token, newEmitter)));
        try {
            ReflectionTestUtils.invokeMethod(manager, "removeEmitter", userId, token, oldEmitter);
            assertEquals(newEmitter, sessions.get(userId).get(token));

            ReflectionTestUtils.invokeMethod(manager, "removeEmitter", userId, token, newEmitter);
            assertFalse(sessions.containsKey(userId));
        } finally {
            sessions.remove(userId);
        }
    }
}
