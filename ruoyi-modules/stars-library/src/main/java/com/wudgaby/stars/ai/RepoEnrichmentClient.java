package com.wudgaby.stars.ai;

import com.wudgaby.stars.domain.StarsRepo;
import com.wudgaby.stars.domain.ai.RepoEnrichmentResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * DeepSeek 仓库 enrichment 客户端
 */
@Component
public class RepoEnrichmentClient {

    private final ChatClient chatClient;
    private final RepoEnrichmentPromptFactory promptFactory;

    public RepoEnrichmentClient(
        @Qualifier("starsEnrichmentChatClient") ChatClient chatClient,
        RepoEnrichmentPromptFactory promptFactory) {
        this.chatClient = chatClient;
        this.promptFactory = promptFactory;
    }

    public RepoEnrichmentResult enrich(StarsRepo repo, String readmeSnippet) {
        return chatClient.prompt()
            .system(promptFactory.systemPrompt())
            .user(promptFactory.userPrompt(repo, readmeSnippet))
            .call()
            .entity(RepoEnrichmentResult.class);
    }
}
