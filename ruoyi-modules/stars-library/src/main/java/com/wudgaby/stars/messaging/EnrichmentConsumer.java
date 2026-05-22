package com.wudgaby.stars.messaging;

import com.wudgaby.stars.service.IStarsEnrichmentService;
import com.wudgaby.stars.support.EnrichmentSlotLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Enrichment Kafka 消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnrichmentConsumer {

    private final IStarsEnrichmentService enrichmentService;
    private final EnrichmentSlotLimiter slotLimiter;

    @KafkaListener(
        topics = "${stars.summary.kafka.request-topic}",
        groupId = "${stars.summary.kafka.consumer-group}",
        containerFactory = "enrichmentListenerContainerFactory")
    public void onMessage(
        @Payload EnrichmentCommand command,
        @Header(KafkaHeaders.RECEIVED_KEY) String messageKey,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset) {
        log.info(
            "Consuming enrichment: userRepoId={}, key={}, partition={}, offset={}, retryCount={}",
            command.userRepoId(),
            messageKey,
            partition,
            offset,
            command.retryCount());

        try {
            slotLimiter.acquire(command.userId());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                "Interrupted while waiting for enrichment slot: userRepoId=" + command.userRepoId(), ex);
        }

        try {
            enrichmentService.process(command);
        } finally {
            slotLimiter.release(command.userId());
        }
    }
}
