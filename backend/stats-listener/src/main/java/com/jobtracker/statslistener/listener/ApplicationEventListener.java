package com.jobtracker.statslistener.listener;

import com.jobtracker.statslistener.event.ApplicationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.OffsetDateTime;

@Component
public class ApplicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(ApplicationEventListener.class);

    private final JdbcTemplate jdbc;

    public ApplicationEventListener(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @KafkaListener(topics = "application-events", groupId = "stats-service")
    public void onApplicationEvent(ApplicationEvent event) {
        log.info("Received event type={} id={}", event.eventType(), event.id());
        try {
            switch (event.eventType()) {
                case "APPLICATION_CREATED" -> jdbc.update(
                        "INSERT INTO applications_snapshot (id, user_id, status, source, created_at, deleted_at, applied_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING",
                        event.id(), event.userId(), event.status(), event.source(),
                        toTs(event.createdAt()), toTs(event.deletedAt()), event.appliedAt());
                case "APPLICATION_UPDATED" -> jdbc.update(
                        "INSERT INTO applications_snapshot (id, user_id, status, source, created_at, deleted_at, applied_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO UPDATE " +
                        "SET status = EXCLUDED.status, source = EXCLUDED.source, deleted_at = EXCLUDED.deleted_at, applied_at = EXCLUDED.applied_at",
                        event.id(), event.userId(), event.status(), event.source(),
                        toTs(event.createdAt()), toTs(event.deletedAt()), event.appliedAt());
                case "APPLICATION_DELETED" -> jdbc.update(
                        "UPDATE applications_snapshot SET deleted_at = ? WHERE id = ?",
                        toTs(event.deletedAt()), event.id());
                default -> log.warn("Unknown event type '{}' for id={} — skipping", event.eventType(), event.id());
            }
            log.debug("Processed event type={} id={}", event.eventType(), event.id());
        } catch (Exception ex) {
            log.error("Failed to process event type={} id={}: {}", event.eventType(), event.id(), ex.getMessage(), ex);
            throw ex; // re-throw so error handler / DLQ kicks in
        }
    }

    private Timestamp toTs(OffsetDateTime odt) {
        return odt == null ? null : Timestamp.from(odt.toInstant());
    }
}
