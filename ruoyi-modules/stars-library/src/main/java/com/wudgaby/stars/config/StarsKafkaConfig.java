package com.wudgaby.stars.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wudgaby.stars.messaging.EnrichmentCommand;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Stars enrichment Kafka 配置
 */
@Configuration
@EnableKafka
public class StarsKafkaConfig {

    @Bean
    ConsumerFactory<String, EnrichmentCommand> enrichmentConsumerFactory(
        KafkaProperties kafkaProperties,
        StarsProperties starsProperties,
        ObjectMapper objectMapper) {
        Map<String, Object> props = consumerPropsWithoutValueDeserializer(kafkaProperties);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, starsProperties.summary().kafka().maxPollRecords());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        JsonDeserializer<EnrichmentCommand> valueDeserializer =
            new JsonDeserializer<>(EnrichmentCommand.class, objectMapper);
        valueDeserializer.addTrustedPackages("com.wudgaby.stars.*");
        valueDeserializer.setUseTypeHeaders(false);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, EnrichmentCommand> enrichmentListenerContainerFactory(
        ConsumerFactory<String, EnrichmentCommand> enrichmentConsumerFactory,
        StarsProperties starsProperties,
        DefaultErrorHandler starsKafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, EnrichmentCommand> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(enrichmentConsumerFactory);
        factory.setConcurrency(starsProperties.summary().maxConcurrentPerPod());
        factory.setCommonErrorHandler(starsKafkaErrorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        return factory;
    }

    @Bean
    ProducerFactory<String, EnrichmentCommand> enrichmentProducerFactory(
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
    KafkaTemplate<String, EnrichmentCommand> enrichmentKafkaTemplate(
        ProducerFactory<String, EnrichmentCommand> enrichmentProducerFactory) {
        return new KafkaTemplate<>(enrichmentProducerFactory);
    }

    @Bean
    DefaultErrorHandler starsKafkaErrorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler(new FixedBackOff(2_000L, 3L));
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }

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
