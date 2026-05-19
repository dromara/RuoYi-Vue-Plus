package com.wudgaby.ticket.services;

import cn.hutool.core.util.StrUtil;
import com.wudgaby.ticket.ai.TicketPromptFactory;
import com.wudgaby.ticket.application.AiDecisionValidator;
import com.wudgaby.ticket.application.TicketDecisionJsonCodec;
import com.wudgaby.ticket.domain.TicketAnalysisCommand;
import com.wudgaby.ticket.domain.TicketRoutingDecision;
import com.wudgaby.ticket.enums.SentimentLabel;
import com.wudgaby.ticket.enums.TicketIntent;
import com.wudgaby.ticket.enums.TicketPriority;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
public class TicketRoutingApplicationService {

    private final ChatClient chatClient;
    private final TicketPromptFactory promptFactory;
    private final AiDecisionValidator validator;
    private final StringRedisTemplate redisTemplate;
    private final TicketDecisionJsonCodec jsonCodec;

    public TicketRoutingApplicationService(
        ChatClient structuredOutputChatClient,
        TicketPromptFactory promptFactory,
        AiDecisionValidator validator,
        StringRedisTemplate redisTemplate,
        TicketDecisionJsonCodec jsonCodec) {
        this.chatClient = structuredOutputChatClient;
        this.promptFactory = promptFactory;
        this.validator = validator;
        this.redisTemplate = redisTemplate;
        this.jsonCodec = jsonCodec;
    }

    @TimeLimiter(name = "ticketDecision")
    @CircuitBreaker(name = "ticketDecision", fallbackMethod = "fallback")
    public CompletableFuture<TicketRoutingDecision> analyze(TicketAnalysisCommand command) {
        return CompletableFuture.supplyAsync(() -> {
            String cacheKey = "ai:ticket:routing:" + command.ticketId() + ":" + command.content().hashCode();
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (StrUtil.isNotBlank(cached)) {
                return jsonCodec.read(cached);
            }

            TicketRoutingDecision decision = chatClient.prompt()
                .system(promptFactory.systemPrompt())
                .user(promptFactory.userPrompt(command))
                .call()
                .entity(TicketRoutingDecision.class);

            TicketRoutingDecision validated = validator.validate(decision);
            redisTemplate.opsForValue().set(cacheKey, jsonCodec.write(validated), Duration.ofMinutes(10));
            return validated;
        });
    }

    @SuppressWarnings("unused")
    private CompletableFuture<TicketRoutingDecision> fallback(TicketAnalysisCommand command, Throwable throwable) {
        return CompletableFuture.completedFuture(
            new TicketRoutingDecision(
                TicketIntent.OTHER,
                TicketPriority.HIGH,
                SentimentLabel.NEGATIVE,
                true,
                "manual-review-queue",
                0,
                java.util.List.of("LLM unavailable, fallback to manual review"),
                java.util.List.of("LLM_DEGRADED"),
                "模型服务不可用，已降级至人工队列"
            )
        );
    }
}
