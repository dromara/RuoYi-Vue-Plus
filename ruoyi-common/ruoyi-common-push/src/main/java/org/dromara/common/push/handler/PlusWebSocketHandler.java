package org.dromara.common.push.handler;

import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.enums.PushSourceEnum;
import org.dromara.common.core.enums.PushTypeEnum;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.push.constant.MessageConstants;
import org.dromara.common.push.core.WebSocketSessionManager;
import org.dromara.common.push.dto.PushDTO;
import org.dromara.common.push.properties.MessageProperties;
import org.dromara.system.api.domain.PushPayloadDTO;
import org.dromara.system.api.model.LoginUser;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.List;

/**
 * WebSocket 请求处理器
 * 处理WebSocket连接建立、消息接收、异常、断开等全生命周期事件
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Slf4j
public class PlusWebSocketHandler extends AbstractWebSocketHandler {

    /**
     * WebSocket 会话管理器
     */
    private final WebSocketSessionManager webSocketSessionManager;

    /**
     * 消息推送配置
     */
    private final MessageProperties messageProperties;

    /**
     * 建立WebSocket连接后触发
     * 校验用户登录信息，注册会话
     *
     * @param session WebSocket会话
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        // 从会话属性中获取登录用户信息和Token
        LoginUser loginUser = (LoginUser) session.getAttributes().get(MessageConstants.LOGIN_USER_KEY);
        String token = (String) session.getAttributes().get(MessageConstants.LOGIN_TOKEN_KEY);

        // 校验用户信息是否为空，无效则直接关闭连接
        if (ObjectUtil.hasNull(loginUser, token)) {
            session.close(CloseStatus.BAD_DATA);
            log.info("[connect] invalid token received. sessionId: {}", session.getId());
            return;
        }

        // 并发安全包装会话，并注册到会话管理器
        webSocketSessionManager.connect(
            loginUser.getUserId(),
            token,
            new ConcurrentWebSocketSessionDecorator(
                session,
                messageProperties.getWebSocketSendTimeLimit(),
                messageProperties.getWebSocketBufferSizeLimit()
            )
        );
        log.info("[connect] sessionId: {}, userId:{}, token:***{}", session.getId(), loginUser.getUserId(), StringUtils.right(token, 8));
    }

    /**
     * 处理客户端发送的文本消息
     * 支持心跳ping/pong，以及自定义消息转发
     *
     * @param session WebSocket会话
     * @param message 文本消息
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        LoginUser loginUser = (LoginUser) session.getAttributes().get(MessageConstants.LOGIN_USER_KEY);
        if (ObjectUtil.isNull(loginUser)) {
            return;
        }

        // 心跳处理：客户端发送ping，服务端回复pong
        if (MessageConstants.PING.equalsIgnoreCase(message.getPayload())) {
            webSocketSessionManager.sendMessage(session, MessageConstants.PONG);
            return;
        }

        // 构建客户端自定义消息并发布
        PushDTO dto = PushDTO.of(List.of(loginUser.getUserId()), PushPayloadDTO.of(
            PushTypeEnum.CUSTOM,
            PushSourceEnum.CLIENT,
            message.getPayload(),
            null
        ));
        webSocketSessionManager.publishMessage(dto);
    }

    /**
     * 处理二进制消息（默认实现）
     */
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        super.handleBinaryMessage(session, message);
    }

    /**
     * 处理Pong心跳响应
     * 维持长连接存活
     */
    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) {
        webSocketSessionManager.sendPongMessage(session);
    }

    /**
     * 传输异常处理
     * 记录异常日志
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[transport error] sessionId: {}, exception:{}", session.getId(), exception.getMessage(), exception);
        LoginUser loginUser = (LoginUser) session.getAttributes().get(MessageConstants.LOGIN_USER_KEY);
        String token = (String) session.getAttributes().get(MessageConstants.LOGIN_TOKEN_KEY);
        if (ObjectUtil.hasNull(loginUser, token)) {
            webSocketSessionManager.closeSession(session, CloseStatus.SERVER_ERROR);
            return;
        }
        webSocketSessionManager.disconnect(loginUser.getUserId(), token, CloseStatus.SERVER_ERROR);
    }

    /**
     * 连接关闭后触发
     * 注销用户会话
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        LoginUser loginUser = (LoginUser) session.getAttributes().get(MessageConstants.LOGIN_USER_KEY);
        String token = (String) session.getAttributes().get(MessageConstants.LOGIN_TOKEN_KEY);

        if (ObjectUtil.hasNull(loginUser, token)) {
            log.info("[disconnect] invalid token received. sessionId: {}", session.getId());
            return;
        }

        // 从会话管理器中移除连接
        webSocketSessionManager.disconnect(loginUser.getUserId(), token);
        log.info("[disconnect] sessionId: {}, userId:{}, token:***{}", session.getId(), loginUser.getUserId(), StringUtils.right(token, 8));
    }

    /**
     * 是否支持分片消息
     * 关闭：不支持分片传输
     */
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
