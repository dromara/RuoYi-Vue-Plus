package com.wudgaby.ticket.messaging;

import com.wudgaby.ticket.abserve.AiMetrics;
import com.wudgaby.ticket.domain.TicketAnalysisCommand;
import com.wudgaby.ticket.services.TicketRoutingApplicationService;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class TicketAnalysisConsumer {

    private static final Logger log = LoggerFactory.getLogger(TicketAnalysisConsumer.class);

    private final TicketRoutingApplicationService applicationService;
    private final RoutingResultPublisher resultPublisher;
    private final Validator validator;
    private final AiMetrics aiMetrics;

    public TicketAnalysisConsumer(
        TicketRoutingApplicationService applicationService,
        RoutingResultPublisher resultPublisher,
        Validator validator,
        AiMetrics aiMetrics) {
        this.applicationService = applicationService;
        this.resultPublisher = resultPublisher;
        this.validator = validator;
        this.aiMetrics = aiMetrics;
    }

    @KafkaListener(
        topics = "${ticket.kafka.analysis-topic}",
        groupId = "${ticket.kafka.consumer-group}",
        containerFactory = "ticketAnalysisListenerContainerFactory")
    public CompletableFuture<Void> onMessage(
        @Payload TicketAnalysisCommand command,
        @Header(KafkaHeaders.RECEIVED_KEY) String messageKey,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset) {
        validate(command);

        log.info(
            "Consuming ticket analysis: ticketId={}, key={}, partition={}, offset={}",
            command.ticketId(),
            messageKey,
            partition,
            offset);

        aiMetrics.incrementRequest();
        Timer.Sample sample = Timer.start();

        return applicationService.analyze(command)
            .thenCompose(decision -> resultPublisher.publish(command.ticketId(), decision))
            .thenAccept(sendResult -> log.info(
                "Ticket analysis completed: ticketId={}, resultPartition={}, resultOffset={}",
                command.ticketId(),
                sendResult.getRecordMetadata().partition(),
                sendResult.getRecordMetadata().offset()))
            .whenComplete((ignored, error) -> sample.stop(aiMetrics.timer()))
            .thenApply(ignored -> null);
    }

    private void validate(TicketAnalysisCommand command) {
        Set<ConstraintViolation<TicketAnalysisCommand>> violations = validator.validate(command);
        if (violations.isEmpty()) {
            return;
        }
        String details = violations.stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .collect(Collectors.joining("; "));
        throw new IllegalArgumentException("Invalid ticket analysis command: " + details);
    }
}
