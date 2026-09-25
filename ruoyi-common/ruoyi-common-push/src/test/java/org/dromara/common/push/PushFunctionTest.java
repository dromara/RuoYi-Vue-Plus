package org.dromara.common.push;

import org.dromara.common.push.annotation.ConditionalOnMessageTransport;
import org.dromara.common.push.condition.MessageTransportCondition;
import org.dromara.common.push.enums.MessageTransportEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

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
}
