package com.wudgaby.ticket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ticket.kafka")
public record TicketKafkaProperties(
    String analysisTopic,
    String resultTopic,
    String consumerGroup,
    String resultConsumerGroup,
    String dltSuffix
) {
}
