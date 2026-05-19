package com.wudgaby.ticket.messaging;

import com.wudgaby.ticket.config.TicketKafkaProperties;
import com.wudgaby.ticket.domain.TicketAnalysisCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class TicketAnalysisProducer {

    private static final Logger log = LoggerFactory.getLogger(TicketAnalysisProducer.class);

    private final KafkaTemplate<String, TicketAnalysisCommand> kafkaTemplate;
    private final TicketKafkaProperties kafkaProperties;

    public TicketAnalysisProducer(
        KafkaTemplate<String, TicketAnalysisCommand> ticketAnalysisKafkaTemplate,
        TicketKafkaProperties kafkaProperties) {
        this.kafkaTemplate = ticketAnalysisKafkaTemplate;
        this.kafkaProperties = kafkaProperties;
    }

    public CompletableFuture<SendResult<String, TicketAnalysisCommand>> enqueue(TicketAnalysisCommand command) {
        return kafkaTemplate.send(kafkaProperties.analysisTopic(), command.ticketId(), command)
            .whenComplete((result, error) -> {
                if (error != null) {
                    log.error("Failed to enqueue ticket analysis for {}", command.ticketId(), error);
                }
                else {
                    log.info(
                        "Enqueued ticket analysis: ticketId={}, topic={}, partition={}, offset={}",
                        command.ticketId(),
                        kafkaProperties.analysisTopic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
    }
}
