package org.dromara.common.push.config;

import org.dromara.common.push.listener.MessageTopicListener;
import org.dromara.common.push.core.WebSocketSessionManager;
import org.dromara.common.push.handler.PlusWebSocketHandler;
import org.dromara.common.push.interceptor.PlusWebSocketInterceptor;
import org.dromara.common.push.properties.MessageProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * WebSocket 消息推送自动装配。
 *
 * @author Lion Li
 */
@EnableWebSocket
@AutoConfiguration(after = MessageAutoConfiguration.class)
@ConditionalOnProperty(prefix = "message", name = "transport", havingValue = "websocket")
public class MessageWebSocketConfiguration {

    @Bean
    public WebSocketConfigurer webSocketConfigurer(HandshakeInterceptor handshakeInterceptor,
                                                   WebSocketHandler webSocketHandler,
                                                   MessageProperties messageProperties) {
        return registry -> registry
            .addHandler(webSocketHandler, messageProperties.getPath())
            .addInterceptors(handshakeInterceptor)
            .setAllowedOrigins(messageProperties.getAllowedOrigins());
    }

    @Bean
    public WebSocketSessionManager webSocketSessionManager() {
        return new WebSocketSessionManager();
    }

    @Bean
    public HandshakeInterceptor handshakeInterceptor() {
        return new PlusWebSocketInterceptor();
    }

    @Bean
    public WebSocketHandler webSocketHandler(WebSocketSessionManager webSocketSessionManager) {
        return new PlusWebSocketHandler(webSocketSessionManager);
    }

    @Bean
    public MessageTopicListener messageTopicListener(WebSocketSessionManager webSocketSessionManager) {
        return new MessageTopicListener(webSocketSessionManager);
    }
}
