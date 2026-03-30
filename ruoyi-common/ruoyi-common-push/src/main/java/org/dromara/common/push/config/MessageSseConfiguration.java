package org.dromara.common.push.config;

import org.dromara.common.push.controller.SseController;
import org.dromara.common.push.core.SseEmitterSessionManager;
import org.dromara.common.push.listener.MessageTopicListener;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * SSE 消息推送自动装配。
 *
 * @author Lion Li
 */
@AutoConfiguration(after = MessageAutoConfiguration.class)
@ConditionalOnProperty(prefix = "message", name = "transport", havingValue = "sse", matchIfMissing = true)
public class MessageSseConfiguration {

    /**
     * 注册 SSE 会话管理器
     * 负责管理用户 SSE 连接、消息发送、会话清理
     *
     * @return SseEmitterSessionManager 实例
     */
    @Bean
    public SseEmitterSessionManager sseEmitterManager() {
        return new SseEmitterSessionManager();
    }

    /**
     * 注册消息主题监听器
     * 监听 Redis 全局消息，用于集群环境下的消息分发
     *
     * @param manager SSE 会话管理器
     * @return MessageTopicListener 实例
     */
    @Bean
    public MessageTopicListener messageTopicListener(SseEmitterSessionManager manager) {
        return new MessageTopicListener(manager);
    }

    /**
     * 注册 SSE 控制器
     * 提供前端建立 SSE 连接的接口
     *
     * @param manager SSE 会话管理器
     * @return SseController 实例
     */
    @Bean
    public SseController sseController(SseEmitterSessionManager manager) {
        return new SseController(manager);
    }
}
