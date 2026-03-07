package com.jobtracker.statslistener.config;

import com.jobtracker.statslistener.event.ApplicationEvent;
import com.jobtracker.statslistener.event.StaleApplicationEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, ApplicationEvent> consumerFactory() {
        JsonDeserializer<ApplicationEvent> deserializer = new JsonDeserializer<>(ApplicationEvent.class, false);
        deserializer.addTrustedPackages("*");

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "stats-service");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ProducerFactory<String, Object> dlqProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> dlqKafkaTemplate(ProducerFactory<String, Object> dlqProducerFactory) {
        return new KafkaTemplate<>(dlqProducerFactory);
    }

    @Bean
    public ConsumerFactory<String, ApplicationEvent> activityConsumerFactory() {
        JsonDeserializer<ApplicationEvent> deserializer = new JsonDeserializer<>(ApplicationEvent.class, false);
        deserializer.addTrustedPackages("*");

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "activity-service");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ApplicationEvent> activityKafkaListenerContainerFactory(
            ConsumerFactory<String, ApplicationEvent> activityConsumerFactory,
            KafkaTemplate<String, Object> dlqKafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dlqKafkaTemplate);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
            log.warn("Activity retry attempt {} for record offset={} due to: {}",
                deliveryAttempt, record.offset(), ex.getMessage())
        );

        ConcurrentKafkaListenerContainerFactory<String, ApplicationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(activityConsumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ApplicationEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, ApplicationEvent> consumerFactory,
            KafkaTemplate<String, Object> dlqKafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dlqKafkaTemplate);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
            log.warn("Retry attempt {} for record offset={} due to: {}",
                deliveryAttempt, record.offset(), ex.getMessage())
        );

        ConcurrentKafkaListenerContainerFactory<String, ApplicationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    // --- staleness-checker consumer (reads application-events) ---

    @Bean
    public ConsumerFactory<String, ApplicationEvent> stalenessConsumerFactory() {
        JsonDeserializer<ApplicationEvent> deserializer = new JsonDeserializer<>(ApplicationEvent.class, false);
        deserializer.addTrustedPackages("*");

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "staleness-checker");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ApplicationEvent> stalenessKafkaListenerContainerFactory(
            ConsumerFactory<String, ApplicationEvent> stalenessConsumerFactory,
            KafkaTemplate<String, Object> dlqKafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dlqKafkaTemplate);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
            log.warn("Staleness retry attempt {} for record offset={} due to: {}",
                deliveryAttempt, record.offset(), ex.getMessage())
        );

        ConcurrentKafkaListenerContainerFactory<String, ApplicationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stalenessConsumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    // --- stale-processor consumer (reads stale-events) ---

    @Bean
    public ConsumerFactory<String, StaleApplicationEvent> staleProcessorConsumerFactory() {
        JsonDeserializer<StaleApplicationEvent> deser = new JsonDeserializer<>(StaleApplicationEvent.class, false);
        deser.addTrustedPackages("*");

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "stale-processor");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deser);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, StaleApplicationEvent> staleProcessorKafkaListenerContainerFactory(
            ConsumerFactory<String, StaleApplicationEvent> staleProcessorConsumerFactory,
            KafkaTemplate<String, Object> dlqKafkaTemplate) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dlqKafkaTemplate);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
            log.warn("Stale-processor retry attempt {} for record offset={} due to: {}",
                deliveryAttempt, record.offset(), ex.getMessage())
        );

        ConcurrentKafkaListenerContainerFactory<String, StaleApplicationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(staleProcessorConsumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
