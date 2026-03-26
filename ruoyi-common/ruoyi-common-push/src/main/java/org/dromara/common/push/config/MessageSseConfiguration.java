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

    @Bean
    public SseEmitterSessionManager sseEmitterManager() {
        return new SseEmitterSessionManager();
    }

    @Bean
    public MessageTopicListener messageTopicListener(SseEmitterSessionManager manager) {
        return new MessageTopicListener(manager);
    }

    @Bean
    public SseController sseController(SseEmitterSessionManager manager) {
        return new SseController(manager);
    }
}
