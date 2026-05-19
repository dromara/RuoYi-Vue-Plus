package com.wudgaby.ticket.ai;

import com.wudgaby.ticket.domain.TicketRoutingDecision;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class TicketDecisionClient {

    private final
    ChatClient chatClient;

    public TicketDecisionClient(ChatClient structuredOutputChatClient) {
        this.chatClient = structuredOutputChatClient;
    }

    public TicketRoutingDecision analyze(String content) {
        return chatClient.prompt()
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
}
