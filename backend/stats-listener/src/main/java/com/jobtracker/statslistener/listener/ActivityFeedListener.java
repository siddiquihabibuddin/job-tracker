package com.jobtracker.statslistener.listener;

import com.jobtracker.statslistener.event.ApplicationEvent;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ActivityFeedListener {

    private static final Logger log = LoggerFactory.getLogger(ActivityFeedListener.class);

    private final JdbcTemplate jdbc;

    public ActivityFeedListener(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @KafkaListener(
        topics = "application-events",
        groupId = "activity-service",
        containerFactory = "activityKafkaListenerContainerFactory"
    )
    @Transactional
    public void onEvent(ApplicationEvent event) {
        String message = buildMessage(event);
        log.info("ActivityFeedListener received {} for appId={}", event.eventType(), event.id());

        jdbc.update(
            """
            INSERT INTO activity_feed (id, user_id, app_id, event_type, message, occurred_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT ON CONSTRAINT uq_feed_app_event_occurred DO NOTHING
            """,
            UUID.randomUUID(),
            event.userId(),
            event.id(),
            event.eventType(),
            message,
            event.occurredAt()
        );
    }

    private String buildMessage(ApplicationEvent event) {
        return switch (event.eventType()) {
            case "APPLICATION_CREATED" -> {
                String role    = event.role()    != null ? event.role()    : "a role";
                String company = event.company() != null ? event.company() : "a company";
                String source  = event.source()  != null ? " via " + event.source() : "";
                yield "Applied for " + role + " at " + company + source;
            }
            case "APPLICATION_UPDATED" -> {
                String status = event.status() != null ? event.status() : "unknown";
                yield "Status changed to " + status;
            }
            case "APPLICATION_DELETED" -> "Application removed";
            default -> event.eventType();
        };
    }
}
