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
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import static org.mockito.Mockito.*;

class WebSocketReconnectTest {

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
