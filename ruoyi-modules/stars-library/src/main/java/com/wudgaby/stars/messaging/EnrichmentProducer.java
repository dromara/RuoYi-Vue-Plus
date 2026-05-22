package com.wudgaby.stars.messaging;

import com.wudgaby.stars.config.StarsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Enrichment 消息生产者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnrichmentProducer {

    private final KafkaTemplate<String, EnrichmentCommand> enrichmentKafkaTemplate;
    private final StarsProperties starsProperties;

    /**
     * 入队 enrichment 任务
     */
    public CompletableFuture<SendResult<String, EnrichmentCommand>> enqueue(EnrichmentCommand command) {
        String topic = starsProperties.summary().kafka().requestTopic();
        return enrichmentKafkaTemplate.send(topic, command.userRepoId().toString(), command)
            .whenComplete((result, error) -> {
                if (error != null) {
                    log.error("Failed to enqueue enrichment: userRepoId={}", command.userRepoId(), error);
                } else {
                    log.info(
                        "Enqueued enrichment: userRepoId={}, topic={}, partition={}, offset={}, retryCount={}",
                        command.userRepoId(),
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        command.retryCount());
                }
            });
    }
}
