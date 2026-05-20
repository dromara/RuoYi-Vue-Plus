package com.wudgaby.ticket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wudgaby.ticket.domain.TicketAnalysisCommand;
import com.wudgaby.ticket.domain.TicketRoutingResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@EnableConfigurationProperties(TicketKafkaProperties.class)
public class KafkaMessagingConfig {

    @Bean
    ConsumerFactory<String, TicketAnalysisCommand> ticketAnalysisConsumerFactory(
        KafkaProperties kafkaProperties,
        ObjectMapper objectMapper) {
        Map<String, Object> props = consumerPropsWithoutValueDeserializer(kafkaProperties);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        JsonDeserializer<TicketAnalysisCommand> valueDeserializer =
            new JsonDeserializer<>(TicketAnalysisCommand.class, objectMapper);
        valueDeserializer.addTrustedPackages("com.wudgaby.ticket.*");
        valueDeserializer.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, TicketAnalysisCommand> ticketAnalysisListenerContainerFactory(
        ConsumerFactory<String, TicketAnalysisCommand> ticketAnalysisConsumerFactory,
        DefaultErrorHandler ticketKafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, TicketAnalysisCommand> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(ticketAnalysisConsumerFactory);
        factory.setCommonErrorHandler(ticketKafkaErrorHandler);
        return factory;
    }

    @Bean
    ConsumerFactory<String, TicketRoutingResult> routingResultConsumerFactory(
        KafkaProperties kafkaProperties,
        ObjectMapper objectMapper) {
        Map<String, Object> props = consumerPropsWithoutValueDeserializer(kafkaProperties);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        JsonDeserializer<TicketRoutingResult> valueDeserializer =
            new JsonDeserializer<>(TicketRoutingResult.class, objectMapper);
        valueDeserializer.addTrustedPackages("com.wudgaby.ticket.*");
        valueDeserializer.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, TicketRoutingResult> routingResultListenerContainerFactory(
        ConsumerFactory<String, TicketRoutingResult> routingResultConsumerFactory,
        DefaultErrorHandler ticketKafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, TicketRoutingResult> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(routingResultConsumerFactory);
        factory.setCommonErrorHandler(ticketKafkaErrorHandler);
        return factory;
    }

    @Bean
    ProducerFactory<String, TicketAnalysisCommand> ticketAnalysisProducerFactory(
        KafkaProperties kafkaProperties,
        ObjectMapper objectMapper) {
        Map<String, Object> props = producerPropsWithoutValueSerializer(kafkaProperties);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(
            props,
            new StringSerializer(),
            new JsonSerializer<>(objectMapper));
    }

    @Bean
    KafkaTemplate<String, TicketAnalysisCommand> ticketAnalysisKafkaTemplate(
        ProducerFactory<String, TicketAnalysisCommand> ticketAnalysisProducerFactory) {
        return new KafkaTemplate<>(ticketAnalysisProducerFactory);
    }

    @Bean
    ProducerFactory<String, TicketRoutingResult> routingResultProducerFactory(
        KafkaProperties kafkaProperties,
        ObjectMapper objectMapper) {
        Map<String, Object> props = producerPropsWithoutValueSerializer(kafkaProperties);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(
            props,
            new StringSerializer(),
            new JsonSerializer<>(objectMapper));
    }

    @Bean
    KafkaTemplate<String, TicketRoutingResult> routingResultKafkaTemplate(
        ProducerFactory<String, TicketRoutingResult> routingResultProducerFactory) {
        return new KafkaTemplate<>(routingResultProducerFactory);
    }

    @Bean
    KafkaTemplate<String, Object> kafkaDltTemplate(KafkaProperties kafkaProperties) {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties()));
    }

    @Bean
    DefaultErrorHandler ticketKafkaErrorHandler(
        KafkaTemplate<String, Object> kafkaDltTemplate,
        TicketKafkaProperties ticketKafkaProperties) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
            kafkaDltTemplate,
            (record, ex) -> new TopicPartition(
                record.topic() + ticketKafkaProperties.dltSuffix(),
                record.partition()));
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(2_000L, 3L));
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }

    /**
     * Explicit JsonDeserializer instances must not also receive JsonDeserializer config via consumer properties.
     */
    private static Map<String, Object> consumerPropsWithoutValueDeserializer(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.remove(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);
        props.remove(JsonDeserializer.TRUSTED_PACKAGES);
        props.remove(JsonDeserializer.USE_TYPE_INFO_HEADERS);
        props.remove(JsonDeserializer.VALUE_DEFAULT_TYPE);
        return props;
    }

    private static Map<String, Object> producerPropsWithoutValueSerializer(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        props.remove(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG);
        return props;
    }
}
