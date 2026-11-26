package org.dromara.common.push;

import org.dromara.common.push.constant.MessageConstants;
import org.dromara.common.push.core.WebSocketSessionManager;
import org.dromara.common.push.handler.PlusWebSocketHandler;
import org.dromara.common.push.properties.MessageProperties;
import org.dromara.system.api.model.LoginUser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import cn.hutool.extra.spring.SpringUtil;
import org.dromara.system.api.domain.PushPayloadDTO;
import org.dromara.common.push.dto.PushDTO;
import org.dromara.common.push.helper.PushHelper;
import org.dromara.common.push.core.PushSessionManager;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import static org.mockito.Mockito.*;

class WebSocketReconnectTest {

    @Test
    void websocketDeliversToAllLiveDevicesAndRemovesFailedConnections() throws Exception {
        var manager = new WebSocketSessionManager(mock(ScheduledExecutorService.class), new MessageProperties());
        var user = new LoginUser();
        var phone = session(user, "phone");
        var broken = session(user, "broken");
        var other = session(user, "other");
        var payload = PushPayloadDTO.of("message", "backend", "order shipped", null);
        var mapper = JsonMapper.builder().build();
        var expected = new TextMessage(mapper.writeValueAsString(payload));
        doThrow(new IOException("closed")).when(broken).sendMessage(any());
        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(JsonMapper.class)).thenReturn(mapper);
            manager.connect(704L, "phone", phone);
            manager.connect(704L, "broken", broken);
            manager.connect(705L, "other", other);
            manager.sendMessage(704L, payload);
            verify(phone).sendMessage(expected);
            verify(other, never()).sendMessage(any());
            verify(broken).close(CloseStatus.SERVER_ERROR);
            manager.sendMessage(payload);
            verify(phone, times(2)).sendMessage(expected);
            verify(other).sendMessage(expected);
            verify(broken, times(1)).sendMessage(any());
            when(phone.isOpen()).thenReturn(false);
            manager.sessionMonitor();
            verify(phone).close(CloseStatus.NORMAL);
        } finally {
            manager.disconnect(704L, "phone");
            manager.disconnect(704L, "broken");
            manager.disconnect(705L, "other");
        }
    }

    @Test
    void enabledPushPreservesAudienceAndDisabledPushSkipsDelivery() {
        var manager = mock(PushSessionManager.class);
        var payload = PushPayloadDTO.of("message", "backend", "order shipped", null);
        var users = List.of(14L, 25L);
        try (var spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(PushSessionManager.class)).thenReturn(manager);
            spring.when(() -> SpringUtil.getProperty("message.enabled", Boolean.class, Boolean.TRUE)).thenReturn(false);
            PushHelper.sendMessage(14L, "order shipped");
            PushHelper.sendMessage("order shipped");
            PushHelper.publishAll("order shipped");
            PushHelper.publishMessage(users, payload);
            verifyNoInteractions(manager);
            spring.when(() -> SpringUtil.getProperty("message.enabled", Boolean.class, Boolean.TRUE)).thenReturn(true);
            PushHelper.publishMessage(null);
            PushHelper.publishMessage(new PushDTO());
            verifyNoInteractions(manager);
            PushHelper.publishMessage(users, payload);
            verify(manager).publishMessage(PushDTO.of(users, payload));
            PushHelper.sendMessage(14L, payload);
            verify(manager).sendMessage(14L, payload);
            PushHelper.sendMessage(payload);
            verify(manager).sendMessage(payload);
            PushHelper.publishAll(payload);
            verify(manager).publishAll(payload);
        }
    }
    @Test
    void clientHeartbeatRepliesLocallyAndTextTargetsOnlyItsOwner() throws Exception {
        var manager = mock(WebSocketSessionManager.class);
        var handler = new PlusWebSocketHandler(manager, new MessageProperties());
        var user = new LoginUser();
        user.setUserId(77L);
        var socket = session(user, "client");
        handler.handleMessage(socket, new TextMessage(MessageConstants.PING));
        verify(manager).sendMessage(socket, MessageConstants.PONG);
        handler.handleMessage(socket, new TextMessage("ready"));
        var sent = org.mockito.ArgumentCaptor.forClass(PushDTO.class);
        verify(manager).publishMessage(sent.capture());
        org.junit.jupiter.api.Assertions.assertEquals(List.of(77L), sent.getValue().getUserIds());
        org.junit.jupiter.api.Assertions.assertEquals("ready", sent.getValue().getPayload().getMessage());
        var anonymous = mock(WebSocketSession.class);
        when(anonymous.getAttributes()).thenReturn(Map.of());
        handler.afterConnectionEstablished(anonymous);
        verify(anonymous).close(CloseStatus.BAD_DATA);
        handler.handleMessage(anonymous, new TextMessage("ignored"));
        verifyNoMoreInteractions(manager);
    }
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldKeepReplacementAfterLateCallback(boolean transportError) throws Exception {
        var properties = new MessageProperties();
        var manager = new WebSocketSessionManager(mock(ScheduledExecutorService.class), properties);
        var handler = new PlusWebSocketHandler(manager, properties);
        var user = new LoginUser();
        user.setUserId(23456L);
        var old = session(user, "old");
        var current = session(user, "current");
        try {
            handler.afterConnectionEstablished(old);
            handler.afterConnectionEstablished(current);
            if (transportError) {
                handler.handleTransportError(old, new IOException("连接已中断"));
            } else {
                handler.afterConnectionClosed(old, CloseStatus.NORMAL);
            }
            verify(current, never()).close(any());
            manager.disconnect(user.getUserId(), "device");
            verify(current).close(CloseStatus.NORMAL);
        } finally {
            manager.disconnect(user.getUserId(), "device");
        }
    }

    private WebSocketSession session(LoginUser user, String id) {
        WebSocketSession session = mock();
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        when(session.getAttributes()).thenReturn(Map.of(MessageConstants.LOGIN_USER_KEY, user,
            MessageConstants.LOGIN_TOKEN_KEY, "device"));
        return session;
    }
}
