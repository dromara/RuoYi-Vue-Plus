package com.wudgaby.ticket.messaging;

import com.wudgaby.ticket.config.TicketKafkaProperties;
import com.wudgaby.ticket.domain.TicketRoutingDecision;
import com.wudgaby.ticket.domain.TicketRoutingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Component
public class RoutingResultPublisher {

    private static final Logger log = LoggerFactory.getLogger(RoutingResultPublisher.class);

    private final KafkaTemplate<String, TicketRoutingResult> kafkaTemplate;
    private final TicketKafkaProperties kafkaProperties;

    public RoutingResultPublisher(
        KafkaTemplate<String, TicketRoutingResult> routingResultKafkaTemplate,
        TicketKafkaProperties kafkaProperties) {
        this.kafkaTemplate = routingResultKafkaTemplate;
        this.kafkaProperties = kafkaProperties;
    }

    public CompletableFuture<SendResult<String, TicketRoutingResult>> publish(
        String ticketId,
        TicketRoutingDecision decision) {
        TicketRoutingResult payload = new TicketRoutingResult(ticketId, Instant.now(), decision);
        return kafkaTemplate.send(kafkaProperties.resultTopic(), ticketId, payload)
            .whenComplete((result, error) -> {
                if (error != null) {
                    log.error("Failed to publish routing result for ticket {}", ticketId, error);
                }
                else {
                    log.debug(
                        "Published routing result for ticket {} to partition {} offset {}",
                        ticketId,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
    }
}
