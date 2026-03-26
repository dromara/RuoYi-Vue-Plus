package org.dromara.common.push.handler;

import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.dto.PushPayload;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.enums.PushSourceEnum;
import org.dromara.common.core.enums.PushTypeEnum;
import org.dromara.common.push.constant.MessageConstants;
import org.dromara.common.push.core.WebSocketSessionManager;
import org.dromara.common.push.dto.PushDTO;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.List;

/**
 * WebSocket Handler。
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Slf4j
public class PlusWebSocketHandler extends AbstractWebSocketHandler {

    private final WebSocketSessionManager webSocketSessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        LoginUser loginUser = (LoginUser) session.getAttributes().get(MessageConstants.LOGIN_USER_KEY);
        String token = (String) session.getAttributes().get(MessageConstants.LOGIN_TOKEN_KEY);
        if (ObjectUtil.hasNull(loginUser, token)) {
            session.close(CloseStatus.BAD_DATA);
            log.info("[connect] invalid token received. sessionId: {}", session.getId());
            return;
        }
        webSocketSessionManager.connect(
            loginUser.getUserId(),
            token,
            new ConcurrentWebSocketSessionDecorator(session, 10 * 1000, 64_000)
        );
        log.info("[connect] sessionId: {}, userId:{}, token:{}", session.getId(), loginUser.getUserId(), token);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        LoginUser loginUser = (LoginUser) session.getAttributes().get(MessageConstants.LOGIN_USER_KEY);
        if (ObjectUtil.isNull(loginUser)) {
            return;
        }
        if (MessageConstants.PING.equalsIgnoreCase(message.getPayload())) {
            webSocketSessionManager.sendMessage(session, MessageConstants.PONG);
            return;
        }
        PushDTO dto = new PushDTO();
        dto.setUserIds(List.of(loginUser.getUserId()));
        dto.setPayload(PushPayload.of(
            PushTypeEnum.CUSTOM,
            PushSourceEnum.CLIENT,
            message.getPayload(),
            null
        ));
        webSocketSessionManager.publishMessage(dto);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        super.handleBinaryMessage(session, message);
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) {
        webSocketSessionManager.sendPongMessage(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[transport error] sessionId: {}, exception:{}", session.getId(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        LoginUser loginUser = (LoginUser) session.getAttributes().get(MessageConstants.LOGIN_USER_KEY);
        String token = (String) session.getAttributes().get(MessageConstants.LOGIN_TOKEN_KEY);
        if (ObjectUtil.hasNull(loginUser, token)) {
            log.info("[disconnect] invalid token received. sessionId: {}", session.getId());
            return;
        }
        webSocketSessionManager.disconnect(loginUser.getUserId(), token);
        log.info("[disconnect] sessionId: {}, userId:{}, token:{}", session.getId(), loginUser.getUserId(), token);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
