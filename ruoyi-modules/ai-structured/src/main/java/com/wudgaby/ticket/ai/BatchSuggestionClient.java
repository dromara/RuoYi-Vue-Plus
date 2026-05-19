package com.wudgaby.ticket.ai;

import com.wudgaby.ticket.domain.TicketRoutingDecision;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BatchSuggestionClient {

    private final ChatClient chatClient;

    public BatchSuggestionClient(ChatClient structuredOutputChatClient) {
        this.chatClient = structuredOutputChatClient;
    }

    public List<TicketRoutingDecision> suggestCandidates(String content) {
        return chatClient.prompt()
            .system("请给出 3 个结构化候选路由方案，按可信度从高到低排序。")
            .user(content)
            .call()
            .entity(new ParameterizedTypeReference<List<TicketRoutingDecision>>() {})
            //.entity(CandidateDecisions.class).items()
            ;
    }
}
