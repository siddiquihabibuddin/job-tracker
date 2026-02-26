package com.jobtracker.apps.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ApplicationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ApplicationEventPublisher.class);
    private static final String TOPIC = "application-events";

    private final KafkaTemplate<String, ApplicationEvent> kafkaTemplate;

    public ApplicationEventPublisher(KafkaTemplate<String, ApplicationEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(ApplicationEvent event) {
        kafkaTemplate.send(TOPIC, event.id().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event type={} id={} to topic {}: {}",
                                event.eventType(), event.id(), TOPIC, ex.getMessage(), ex);
                    } else {
                        log.debug("Published event type={} id={} to partition={} offset={}",
                                event.eventType(), event.id(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    /**
     * Synchronous publish — blocks up to 10 seconds waiting for broker ack.
     * Throws if the send fails or times out, allowing the caller to handle per-event errors.
     */
    public void publishSync(ApplicationEvent event) throws Exception {
        kafkaTemplate.send(TOPIC, event.id().toString(), event)
                .get(10, TimeUnit.SECONDS);
        log.debug("Outbox sync-published event type={} id={}", event.eventType(), event.id());
    }
}
