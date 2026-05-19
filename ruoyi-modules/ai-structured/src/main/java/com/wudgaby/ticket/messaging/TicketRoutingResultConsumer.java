package com.wudgaby.ticket.messaging;

import com.wudgaby.ticket.domain.TicketRoutingResult;
import com.wudgaby.ticket.services.TicketRoutingResultHandler;
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
import java.util.stream.Collectors;

@Component
public class TicketRoutingResultConsumer {

    private static final Logger log = LoggerFactory.getLogger(TicketRoutingResultConsumer.class);

    private final TicketRoutingResultHandler resultHandler;
    private final Validator validator;

    public TicketRoutingResultConsumer(TicketRoutingResultHandler resultHandler, Validator validator) {
        this.resultHandler = resultHandler;
        this.validator = validator;
    }

    @KafkaListener(
        topics = "${ticket.kafka.result-topic}",
        groupId = "${ticket.kafka.result-consumer-group}",
        containerFactory = "routingResultListenerContainerFactory")
    public void onMessage(
        @Payload TicketRoutingResult result,
        @Header(KafkaHeaders.RECEIVED_KEY) String messageKey,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset) {
        validate(result);

        log.info(
            "Consuming routing result: ticketId={}, key={}, partition={}, offset={}",
            result.ticketId(),
            messageKey,
            partition,
            offset);

        resultHandler.handle(result);
    }

    private void validate(TicketRoutingResult result) {
        Set<ConstraintViolation<TicketRoutingResult>> violations = validator.validate(result);
        if (violations.isEmpty()) {
            return;
        }
        String details = violations.stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .collect(Collectors.joining("; "));
        throw new IllegalArgumentException("Invalid routing result: " + details);
    }
}
