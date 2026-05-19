package com.wudgaby.ticket.controller;

import com.wudgaby.ticket.ai.TicketDecisionClient;
import com.wudgaby.ticket.domain.TicketAnalysisCommand;
import com.wudgaby.ticket.domain.TicketAnalysisQueuedResponse;
import com.wudgaby.ticket.domain.TicketRoutingDecision;
import com.wudgaby.ticket.messaging.TicketAnalysisProducer;
import com.wudgaby.ticket.services.TicketRoutingApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketRoutingController {

    private final TicketRoutingApplicationService applicationService;
    private final TicketDecisionClient ticketDecisionClient;
    private final TicketAnalysisProducer analysisProducer;

    @PostMapping("/analyze")
    public CompletableFuture<TicketRoutingDecision> analyze(@Valid @RequestBody TicketAnalysisCommand command) {
        return applicationService.analyze(command);
    }

    @GetMapping("/question")
    public TicketRoutingDecision analyze(String question) {
        return ticketDecisionClient.analyze(question);
    }

    /**
     * 异步入队：写入 ticket-analysis，由 {@link com.wudgaby.ticket.messaging.TicketAnalysisConsumer} 消费，
     * 结果发布到 ticket-routing-result。
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
