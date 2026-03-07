package com.jobtracker.statslistener.listener;

import com.jobtracker.statslistener.event.StaleApplicationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StaleProcessorListener {

    private static final Logger log = LoggerFactory.getLogger(StaleProcessorListener.class);

    private final JdbcTemplate jdbc;

    public StaleProcessorListener(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @KafkaListener(
        topics = "stale-events",
        groupId = "stale-processor",
        containerFactory = "staleProcessorKafkaListenerContainerFactory")
    @Transactional
    public void onStaleEvent(StaleApplicationEvent event) {
        log.info("Processing stale flag for appId={} userId={} days={}", event.appId(), event.userId(), event.daysSinceLastEvent());
        jdbc.update("""
            INSERT INTO stale_flags (app_id, user_id, company, role, status, days_stale, flagged_at)
            VALUES (?, ?, ?, ?, ?, ?, NOW())
            ON CONFLICT (app_id) DO NOTHING
            """,
            event.appId(), event.userId(), event.company(),
            event.role(), event.status(), event.daysSinceLastEvent());
    }
}
