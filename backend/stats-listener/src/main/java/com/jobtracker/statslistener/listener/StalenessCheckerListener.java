package com.jobtracker.statslistener.listener;

import com.jobtracker.statslistener.event.ApplicationEvent;
import com.jobtracker.statslistener.event.StaleApplicationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class StalenessCheckerListener {

    private static final Logger log = LoggerFactory.getLogger(StalenessCheckerListener.class);
    private static final int STALE_THRESHOLD_DAYS = 14;

    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public StalenessCheckerListener(JdbcTemplate jdbc, KafkaTemplate<String, Object> kafkaTemplate) {
        this.jdbc = jdbc;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(
        topics = "application-events",
        groupId = "staleness-checker",
        containerFactory = "stalenessKafkaListenerContainerFactory")
    @Transactional
    public void onEvent(ApplicationEvent event) {
        log.debug("StalenessCheckerListener processing event type={} userId={}", event.eventType(), event.userId());
        jdbc.query("""
            SELECT id, company, role, status,
                   EXTRACT(DAY FROM NOW() - COALESCE(applied_at, created_at::date))::INT AS days_stale
            FROM applications_snapshot
            WHERE user_id = ?
              AND deleted_at IS NULL
              AND status = 'APPLIED'
              AND COALESCE(applied_at, created_at::date) < NOW() - INTERVAL '14 days'
              AND id NOT IN (SELECT app_id FROM stale_flags WHERE resolved_at IS NULL)
            """,
            (RowCallbackHandler) rs -> {
                UUID appId = UUID.fromString(rs.getString("id"));
                StaleApplicationEvent stale = new StaleApplicationEvent(
                    appId,
                    event.userId(),
                    rs.getString("company"),
                    rs.getString("role"),
                    rs.getString("status"),
                    rs.getInt("days_stale"),
                    OffsetDateTime.now());
                log.info("Publishing stale event for appId={} daysSinceLastEvent={}", appId, stale.daysSinceLastEvent());
                kafkaTemplate.send("stale-events", stale.appId().toString(), stale);
            },
            event.userId());
    }
}
