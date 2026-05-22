package com.wudgaby.stars.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Stars DeepSeek ChatClient 配置
 */
@Configuration
public class StarsAiConfig {

    @Bean
    ChatClient starsEnrichmentChatClient(DeepSeekChatModel deepSeekChatModel) {
        return ChatClient.builder(deepSeekChatModel).build();
    }
}
