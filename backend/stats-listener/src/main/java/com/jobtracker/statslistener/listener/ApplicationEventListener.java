package com.jobtracker.statslistener.listener;

import com.jobtracker.statslistener.event.ApplicationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

@Component
public class ApplicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(ApplicationEventListener.class);

    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    public ApplicationEventListener(JdbcTemplate jdbc, StringRedisTemplate redis) {
        this.jdbc = jdbc;
        this.redis = redis;
    }

    @Transactional
    @KafkaListener(topics = "application-events", groupId = "stats-service")
    public void onApplicationEvent(ApplicationEvent event) {
        log.info("Received event type={} id={}", event.eventType(), event.id());
        try {
            switch (event.eventType()) {
                case "APPLICATION_CREATED" -> {
                    jdbc.update(
                            "INSERT INTO applications_snapshot (id, user_id, status, source, created_at, deleted_at, applied_at, role, got_call) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING",
                            event.id(), event.userId(), event.status(), event.source(),
                            toTs(event.createdAt()), toTs(event.deletedAt()), event.appliedAt(), event.role(),
                            event.gotCall() != null && event.gotCall());
                    jdbc.update(
                            "UPDATE applications_snapshot SET last_event_at = ?, company = ? WHERE id = ?",
                            toTs(event.occurredAt()), event.company(), event.id());
                    LocalDate newDate = effectiveDate(event.appliedAt(), event.createdAt());
                    recomputeMonthly(event.userId(), newDate.getYear(), newDate.getMonthValue());
                    recomputeWeekly(event.userId(), newDate);
                    recomputeRole(event.userId(), newDate.getYear(), newDate.getMonthValue());
                }
                case "APPLICATION_UPDATED" -> {
                    LocalDate oldDate = readEffectiveDate(event.id());
                    jdbc.update(
                            "INSERT INTO applications_snapshot (id, user_id, status, source, created_at, deleted_at, applied_at, role, got_call) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO UPDATE " +
                            "SET status = EXCLUDED.status, source = EXCLUDED.source, deleted_at = EXCLUDED.deleted_at, applied_at = EXCLUDED.applied_at, role = EXCLUDED.role, got_call = EXCLUDED.got_call",
                            event.id(), event.userId(), event.status(), event.source(),
                            toTs(event.createdAt()), toTs(event.deletedAt()), event.appliedAt(), event.role(),
                            event.gotCall() != null && event.gotCall());
                    jdbc.update(
                            "UPDATE applications_snapshot SET last_event_at = ?, company = ? WHERE id = ?",
                            toTs(event.occurredAt()), event.company(), event.id());
                    jdbc.update(
                            "UPDATE stale_flags SET resolved_at = NOW() WHERE app_id = ? AND resolved_at IS NULL",
                            event.id());
                    LocalDate newDate = effectiveDate(event.appliedAt(), event.createdAt());
                    recomputeMonthly(event.userId(), newDate.getYear(), newDate.getMonthValue());
                    recomputeWeekly(event.userId(), newDate);
                    recomputeRole(event.userId(), newDate.getYear(), newDate.getMonthValue());
                    if (oldDate != null && !oldDate.equals(newDate)) {
                        recomputeMonthly(event.userId(), oldDate.getYear(), oldDate.getMonthValue());
                        recomputeWeekly(event.userId(), oldDate);
                        recomputeRole(event.userId(), oldDate.getYear(), oldDate.getMonthValue());
                    }
                }
                case "APPLICATION_DELETED" -> {
                    LocalDate oldDate = readEffectiveDate(event.id());
                    jdbc.update(
                            "UPDATE applications_snapshot SET deleted_at = ? WHERE id = ?",
                            toTs(event.deletedAt()), event.id());
                    jdbc.update(
                            "UPDATE applications_snapshot SET last_event_at = ? WHERE id = ?",
                            toTs(event.occurredAt()), event.id());
                    jdbc.update(
                            "UPDATE stale_flags SET resolved_at = NOW() WHERE app_id = ? AND resolved_at IS NULL",
                            event.id());
                    if (oldDate != null) {
                        recomputeMonthly(event.userId(), oldDate.getYear(), oldDate.getMonthValue());
                        recomputeWeekly(event.userId(), oldDate);
                        recomputeRole(event.userId(), oldDate.getYear(), oldDate.getMonthValue());
                    }
                }
                default -> log.warn("Unknown event type '{}' for id={} — skipping", event.eventType(), event.id());
            }
            evictStatsCache(event.userId());
            log.debug("Processed event type={} id={}", event.eventType(), event.id());
        } catch (Exception ex) {
            log.error("Failed to process event type={} id={}: {}", event.eventType(), event.id(), ex.getMessage(), ex);
            throw ex; // re-throw so error handler / DLQ kicks in
        }
    }

    private void evictStatsCache(UUID userId) {
        String prefix = userId.toString() + ":";
        for (String cacheName : new String[]{"stats-summary", "stats-trend", "stats-breakdown", "stats-roles", "stats-companies"}) {
            var keys = redis.keys(cacheName + "::" + prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
                log.debug("Evicted {} keys from cache '{}'", keys.size(), cacheName);
            }
        }
    }

    private LocalDate readEffectiveDate(UUID id) {
        return jdbc.query(
                "SELECT COALESCE(applied_at, created_at::date) FROM applications_snapshot WHERE id = ?",
                rs -> rs.next() ? rs.getObject(1, LocalDate.class) : null,
                id);
    }

    private void recomputeMonthly(UUID userId, int year, int month) {
        jdbc.update("DELETE FROM agg_monthly WHERE user_id=? AND year=? AND month=?", userId, year, month);
        jdbc.update("""
                INSERT INTO agg_monthly (user_id, year, month, status, cnt)
                SELECT user_id,
                       EXTRACT(year  FROM COALESCE(applied_at, created_at::date))::smallint,
                       EXTRACT(month FROM COALESCE(applied_at, created_at::date))::smallint,
                       status, COUNT(*)
                FROM applications_snapshot
                WHERE user_id=? AND deleted_at IS NULL
                  AND EXTRACT(year  FROM COALESCE(applied_at, created_at::date))=?
                  AND EXTRACT(month FROM COALESCE(applied_at, created_at::date))=?
                GROUP BY 1,2,3,4""",
                userId, year, month);
    }

    private void recomputeWeekly(UUID userId, LocalDate anyDateInWeek) {
        LocalDate weekStart = anyDateInWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        jdbc.update("DELETE FROM agg_weekly WHERE user_id=? AND week_start=?", userId, Date.valueOf(weekStart));
        jdbc.update("""
                INSERT INTO agg_weekly (user_id, week_start, cnt, calls_cnt)
                SELECT user_id,
                       date_trunc('week', COALESCE(applied_at, created_at::date))::date,
                       COUNT(*),
                       COUNT(*) FILTER (WHERE got_call = true)
                FROM applications_snapshot
                WHERE user_id=? AND deleted_at IS NULL
                  AND date_trunc('week', COALESCE(applied_at, created_at::date))::date=?
                GROUP BY 1,2""",
                userId, Date.valueOf(weekStart));
    }

    private void recomputeRole(UUID userId, int year, int month) {
        jdbc.update("DELETE FROM agg_role WHERE user_id=? AND year=? AND month=?", userId, year, month);
        jdbc.update("""
                INSERT INTO agg_role (user_id, year, month, category, cnt)
                SELECT user_id,
                       EXTRACT(year  FROM COALESCE(applied_at, created_at::date))::smallint,
                       EXTRACT(month FROM COALESCE(applied_at, created_at::date))::smallint,
                       CASE
                           WHEN role ILIKE '%manager%'   OR role ILIKE '%mgr%'                            THEN 'MANAGER'
                           WHEN role ILIKE '%architect%'                                                  THEN 'ARCHITECT'
                           WHEN role ILIKE '%engineer%' OR role ILIKE '%developer%' OR role ILIKE '%dev%' THEN 'ENGINEER_DEV'
                           ELSE 'OTHER'
                       END,
                       COUNT(*)
                FROM applications_snapshot
                WHERE user_id=? AND deleted_at IS NULL
                  AND EXTRACT(year  FROM COALESCE(applied_at, created_at::date))=?
                  AND EXTRACT(month FROM COALESCE(applied_at, created_at::date))=?
                GROUP BY 1,2,3,4""",
                userId, year, month);
    }

    private LocalDate effectiveDate(LocalDate appliedAt, OffsetDateTime createdAt) {
        return appliedAt != null ? appliedAt : createdAt.toLocalDate();
    }

    private Timestamp toTs(OffsetDateTime odt) {
        return odt == null ? null : Timestamp.from(odt.toInstant());
    }
}
