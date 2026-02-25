package com.jobtracker.stats.service;

import com.jobtracker.stats.api.dto.StatsSummaryDto;
import com.jobtracker.stats.api.dto.TrendPointDto;
import com.jobtracker.stats.api.dto.TrendResponseDto;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class StatsService {

    private static final Logger log = LoggerFactory.getLogger(StatsService.class);
    private static final UUID DEMO_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final JdbcTemplate jdbc;
    private final Counter queriesCounter;

    public StatsService(JdbcTemplate jdbc, MeterRegistry meterRegistry) {
        this.jdbc = jdbc;
        this.queriesCounter = Counter.builder("stats.queries.total")
                .description("Total stats queries served")
                .register(meterRegistry);
    }

    public StatsSummaryDto getSummary(int windowDays) {
        log.info("Serving summary query windowDays={}", windowDays);
        queriesCounter.increment();
        String interval = windowDays + " days";

        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM applications_snapshot WHERE user_id=? AND deleted_at IS NULL AND created_at >= NOW() - ?::interval",
                Long.class, DEMO_USER_ID, interval);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        jdbc.query(
                "SELECT status, COUNT(*) FROM applications_snapshot WHERE user_id=? AND deleted_at IS NULL AND created_at >= NOW() - ?::interval GROUP BY status",
                (RowCallbackHandler) rs -> byStatus.put(rs.getString(1), rs.getLong(2)),
                DEMO_USER_ID, interval);

        Map<String, Long> bySource = new LinkedHashMap<>();
        jdbc.query(
                "SELECT COALESCE(source, 'Unknown'), COUNT(*) FROM applications_snapshot WHERE user_id=? AND deleted_at IS NULL AND created_at >= NOW() - ?::interval GROUP BY 1",
                (RowCallbackHandler) rs -> bySource.put(rs.getString(1), rs.getLong(2)),
                DEMO_USER_ID, interval);

        return new StatsSummaryDto(
                windowDays,
                total != null ? total : 0L,
                byStatus,
                bySource,
                OffsetDateTime.now().toString());
    }

    public TrendResponseDto getTrend(int weeks) {
        log.info("Serving trend query weeks={}", weeks);
        queriesCounter.increment();
        String interval = weeks + " weeks";

        Map<LocalDate, Long> dbResults = new LinkedHashMap<>();
        jdbc.query(
                "SELECT date_trunc('week', created_at) AS week_start, COUNT(*) " +
                "FROM applications_snapshot " +
                "WHERE user_id=? AND deleted_at IS NULL AND created_at >= NOW() - ?::interval " +
                "GROUP BY 1 ORDER BY 1",
                (RowCallbackHandler) rs -> {
                    LocalDate weekStart = rs.getTimestamp(1).toLocalDateTime().toLocalDate();
                    dbResults.put(weekStart, rs.getLong(2));
                },
                DEMO_USER_ID, interval);

        LocalDate currentWeekMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<TrendPointDto> points = new ArrayList<>(weeks);
        for (int i = weeks - 1; i >= 0; i--) {
            LocalDate weekStart = currentWeekMonday.minusWeeks(i);
            LocalDate weekEnd = weekStart.plusDays(6);
            long count = dbResults.getOrDefault(weekStart, 0L);
            points.add(new TrendPointDto(weekStart.format(DATE_FMT), weekEnd.format(DATE_FMT), count));
        }

        return new TrendResponseDto("week", points);
    }
}
