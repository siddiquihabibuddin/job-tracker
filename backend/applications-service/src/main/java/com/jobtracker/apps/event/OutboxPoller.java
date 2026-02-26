package com.jobtracker.apps.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.apps.domain.model.OutboxEvent;
import com.jobtracker.apps.domain.repo.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxEventRepository outboxRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public OutboxPoller(OutboxEventRepository outboxRepo,
                        ApplicationEventPublisher eventPublisher,
                        ObjectMapper objectMapper) {
        this.outboxRepo = outboxRepo;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:5000}")
    public void poll() {
        var pending = outboxRepo.findByPublishedAtIsNullOrderByCreatedAtAsc();
        if (pending.isEmpty()) return;

        log.debug("Outbox poller found {} pending event(s)", pending.size());

        for (OutboxEvent row : pending) {
            try {
                ApplicationEvent event = objectMapper.readValue(row.getPayload(), ApplicationEvent.class);
                eventPublisher.publishSync(event);
                row.setPublishedAt(OffsetDateTime.now());
                outboxRepo.save(row);
                log.debug("Outbox published event type={} id={}", row.getEventType(), row.getId());
            } catch (Exception ex) {
                log.error("Outbox failed to publish event id={} type={}: {}",
                        row.getId(), row.getEventType(), ex.getMessage(), ex);
                // leave publishedAt null — will retry on next tick
            }
        }
    }
}
