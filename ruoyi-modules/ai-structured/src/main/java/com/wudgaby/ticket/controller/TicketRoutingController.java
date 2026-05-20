package com.wudgaby.ticket.controller;

import com.wudgaby.ticket.ai.TicketDecisionClient;
import com.wudgaby.ticket.api.openai.OpenAiStreamResponseMapper;
import com.wudgaby.ticket.domain.TicketAnalysisCommand;
import com.wudgaby.ticket.domain.TicketAnalysisQueuedResponse;
import com.wudgaby.ticket.domain.TicketRoutingDecision;
import com.wudgaby.ticket.domain.TicketRoutingResult;
import com.wudgaby.ticket.messaging.TicketAnalysisProducer;
import com.wudgaby.ticket.services.TicketRoutingApplicationService;
import com.wudgaby.ticket.services.TicketRoutingResultHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketRoutingController {

    private final TicketRoutingApplicationService applicationService;
    private final TicketDecisionClient ticketDecisionClient;
    private final TicketAnalysisProducer analysisProducer;
    private final TicketRoutingResultHandler routingResultHandler;
    private final OpenAiStreamResponseMapper openAiStreamResponseMapper;

    @PostMapping("/async")
    public CompletableFuture<String> async() {
        log.info("async demo");
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Async response at " + System.currentTimeMillis();
        });
    }

    @PostMapping("/analyze")
    public CompletableFuture<TicketRoutingDecision> analyze(@Valid @RequestBody TicketAnalysisCommand command) {
        log.info("analyze invoked, ticketId={}", command.ticketId());
        return applicationService.analyze(command);
    }

    @GetMapping("/question")
    public TicketRoutingDecision question(String question) {
        return ticketDecisionClient.analyze(question);
    }

    @GetMapping(value = "/sse1", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sse1(@RequestParam String question) {
        return ticketDecisionClient.stream(question);
    }

    /**
     * SSE 流式分析。浏览器请用 EventSource 或 curl -N；路径 {@code /see} 为兼容别名。
     */
    @GetMapping(value = {"/sse2"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sse2(@RequestParam String question) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        ticketDecisionClient.stream(question).subscribe(
            chunk -> {
                try {
                    emitter.send(SseEmitter.event().data(chunk));
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            },
            emitter::completeWithError,
            emitter::complete
        );
        return emitter;
    }

    /**
     * OpenAI Chat Completions 兼容 SSE（{@code /v1/chat/completions} stream=true 同款 data 行）。
     */
    @GetMapping(value = "/sse3", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> sse3(@RequestParam String question) {
        return openAiStreamResponseMapper.toSse(ticketDecisionClient.stream(question));
    }

    /**
     * 查询异步入队后的路由结果（由 {@link com.wudgaby.ticket.messaging.TicketRoutingResultConsumer} 写入 Redis）。
     */
    @GetMapping("/{ticketId}/routing-result")
    public ResponseEntity<TicketRoutingResult> routingResult(@PathVariable String ticketId) {
        return routingResultHandler.findByTicketId(ticketId)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Routing result not ready"));
    }

    /**
     * 异步入队：写入 ticket-analysis，由 {@link com.wudgaby.ticket.messaging.TicketAnalysisConsumer} 消费，
     * 结果发布到 ticket-routing-result，再由 {@link com.wudgaby.ticket.messaging.TicketRoutingResultConsumer} 落库。
     */
    @PostMapping("/queue")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CompletableFuture<TicketAnalysisQueuedResponse> queue(@Valid @RequestBody TicketAnalysisCommand command) {
        return analysisProducer.enqueue(command)
            .thenApply(sendResult -> new TicketAnalysisQueuedResponse(
                command.ticketId(),
                sendResult.getRecordMetadata().topic(),
                sendResult.getRecordMetadata().partition(),
                sendResult.getRecordMetadata().offset()));
    }
}
