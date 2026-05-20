package com.wudgaby.ticket.ai;

import com.wudgaby.ticket.domain.TicketRoutingDecision;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class TicketDecisionClient {

    private final ChatClient structuredChatClient;
    private final ChatClient streamingChatClient;

    public TicketDecisionClient(
        ChatClient structuredOutputChatClient,
        @Qualifier("deepseekChatClient") ChatClient deepseekChatClient) {
        this.structuredChatClient = structuredOutputChatClient;
        this.streamingChatClient = deepseekChatClient;
    }

    public TicketRoutingDecision analyze(String content) {
        return structuredChatClient.prompt()
            .system("""
                你是资深客服路由引擎。
                你的任务是从用户表达中识别工单意图、优先级、情绪和是否需要人工介入。
                输出必须可供系统直接消费。
                """)
            .user("""
                请分析以下用户输入：
                %s
                """.formatted(content))
            .call()
            .entity(TicketRoutingDecision.class);
    }

    /**
     * 流式输出（勿使用 structured JSON 客户端：json_object + thinking 与 stream 不兼容）。
     */
    public Flux<String> stream(String content) {
        return streamingChatClient.prompt()
            .system("""
                你是资深客服路由引擎。
                你的任务是从用户表达中识别工单意图、优先级、情绪和是否需要人工介入。
                请用简洁中文逐段说明分析过程与结论。
                """)
            .user("""
                请分析以下用户输入：
                %s
                """.formatted(content))
            .stream()
            .content();
    }
}
