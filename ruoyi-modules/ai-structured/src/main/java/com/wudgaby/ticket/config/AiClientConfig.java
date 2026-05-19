package com.wudgaby.ticket.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class AiClientConfig {

    /**
     * DeepSeek thinking 模式需通过 OpenAI 兼容 API 的 extra_body 传入 thinking 参数。
     *
     * @see <a href="https://api-docs.deepseek.com/guides/thinking_mode">DeepSeek Thinking Mode</a>
     */
    private static final OpenAiChatOptions THINKING_OPTIONS = OpenAiChatOptions.builder()
        .reasoningEffort("high")
        .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_OBJECT, null))
        .extraBody(Map.of("thinking", Map.of("type", "enabled")))
        .build();

    /**
     * thinking + JSON 输出（deepseek-v4-pro）。
     * 勿使用 ENABLE_NATIVE_STRUCTURED_OUTPUT：会向 API 发送 json_schema，DeepSeek 不支持。
     * .entity() 走 BeanOutputConverter（提示词约束 + json_object）。
     */
    @Bean
    ChatClient structuredOutputChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
            .defaultOptions(THINKING_OPTIONS)
            //.defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
            .build();
    }

    /**
     * 原生 DeepSeek API（如 deepseek-chat / deepseek-reasoner）。
     */
    @Bean
    ChatClient deepseekChatClient(DeepSeekChatModel deepSeekChatModel) {
        return ChatClient.builder(deepSeekChatModel).build();
    }
}
