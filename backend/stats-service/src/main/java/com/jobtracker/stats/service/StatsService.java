package com.jobtracker.stats.service;

import com.jobtracker.stats.api.dto.BreakdownResponseDto;
import com.jobtracker.stats.api.dto.BreakdownRowDto;
import com.jobtracker.stats.api.dto.OpenWindowsDto;
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
                "SELECT COUNT(*) FROM applications_snapshot WHERE user_id=? AND deleted_at IS NULL AND COALESCE(applied_at, created_at::date) >= (NOW() - ?::interval)::date",
                Long.class, DEMO_USER_ID, interval);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        jdbc.query(
                "SELECT status, COUNT(*) FROM applications_snapshot WHERE user_id=? AND deleted_at IS NULL AND COALESCE(applied_at, created_at::date) >= (NOW() - ?::interval)::date GROUP BY status",
                (RowCallbackHandler) rs -> byStatus.put(rs.getString(1), rs.getLong(2)),
                DEMO_USER_ID, interval);

        Map<String, Long> bySource = new LinkedHashMap<>();
        jdbc.query(
                "SELECT COALESCE(source, 'Unknown'), COUNT(*) FROM applications_snapshot WHERE user_id=? AND deleted_at IS NULL AND COALESCE(applied_at, created_at::date) >= (NOW() - ?::interval)::date GROUP BY 1",
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
                "SELECT date_trunc('week', COALESCE(applied_at, created_at::date)) AS week_start, COUNT(*) " +
                "FROM applications_snapshot " +
                "WHERE user_id=? AND deleted_at IS NULL AND COALESCE(applied_at, created_at::date) >= (NOW() - ?::interval)::date " +
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

    public BreakdownResponseDto getBreakdown(String groupBy, Integer year) {
        log.info("Serving breakdown query groupBy={} year={}", groupBy, year);
        queriesCounter.increment();

        boolean byMonth = "month".equalsIgnoreCase(groupBy);

        List<BreakdownRowDto> rows = new ArrayList<>();
        String[] MONTH_LABELS = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};

        if (byMonth) {
            String sql =
                "SELECT EXTRACT(month FROM COALESCE(applied_at, created_at::date))::int AS period_num, " +
                "  COUNT(*) AS total_applied, " +
                "  COUNT(*) FILTER (WHERE status = 'REJECTED') AS total_rejected, " +
                "  COUNT(*) FILTER (WHERE status NOT IN ('REJECTED','ACCEPTED','WITHDRAWN')) AS total_open " +
                "FROM applications_snapshot " +
                "WHERE user_id=? AND deleted_at IS NULL " +
                "  AND EXTRACT(year FROM COALESCE(applied_at, created_at::date)) = ? " +
                "GROUP BY 1 ORDER BY 1";

            int targetYear = (year != null) ? year : LocalDate.now().getYear();
            jdbc.query(sql, (RowCallbackHandler) rs -> {
                int m = rs.getInt(1);
                rows.add(new BreakdownRowDto(
                    MONTH_LABELS[m - 1],
                    m,
                    rs.getLong(2),
                    rs.getLong(3),
                    rs.getLong(4)
                ));
            }, DEMO_USER_ID, targetYear);

            List<BreakdownRowDto> full = new ArrayList<>();
            for (int m = 1; m <= 12; m++) {
                final int mm = m;
                BreakdownRowDto found = rows.stream().filter(r -> r.periodNum() == mm).findFirst().orElse(null);
                full.add(found != null ? found : new BreakdownRowDto(MONTH_LABELS[m - 1], m, 0, 0, 0));
            }

            OpenWindowsDto windows = queryOpenWindows();
            return new BreakdownResponseDto("month", targetYear, full, windows);

        } else {
            String sql =
                "SELECT EXTRACT(year FROM COALESCE(applied_at, created_at::date))::int AS period_num, " +
                "  COUNT(*) AS total_applied, " +
                "  COUNT(*) FILTER (WHERE status = 'REJECTED') AS total_rejected, " +
                "  COUNT(*) FILTER (WHERE status NOT IN ('REJECTED','ACCEPTED','WITHDRAWN')) AS total_open " +
                "FROM applications_snapshot " +
                "WHERE user_id=? AND deleted_at IS NULL " +
                "GROUP BY 1 ORDER BY 1";

            jdbc.query(sql, (RowCallbackHandler) rs -> {
                int y = rs.getInt(1);
                rows.add(new BreakdownRowDto(
                    String.valueOf(y),
                    y,
                    rs.getLong(2),
                    rs.getLong(3),
                    rs.getLong(4)
                ));
            }, DEMO_USER_ID);

            OpenWindowsDto windows = queryOpenWindows();
            return new BreakdownResponseDto("year", null, rows, windows);
        }
    }

    private OpenWindowsDto queryOpenWindows() {
        String sql =
            "SELECT " +
            "  COUNT(*) FILTER (WHERE COALESCE(applied_at, created_at::date) >= CURRENT_DATE - 7   AND status NOT IN ('REJECTED','ACCEPTED','WITHDRAWN')) AS last7d, " +
            "  COUNT(*) FILTER (WHERE COALESCE(applied_at, created_at::date) >= CURRENT_DATE - 15  AND status NOT IN ('REJECTED','ACCEPTED','WITHDRAWN')) AS last15d, " +
            "  COUNT(*) FILTER (WHERE COALESCE(applied_at, created_at::date) >= CURRENT_DATE - 30  AND status NOT IN ('REJECTED','ACCEPTED','WITHDRAWN')) AS last30d, " +
            "  COUNT(*) FILTER (WHERE COALESCE(applied_at, created_at::date) >= CURRENT_DATE - 92  AND status NOT IN ('REJECTED','ACCEPTED','WITHDRAWN')) AS last3m, " +
            "  COUNT(*) FILTER (WHERE COALESCE(applied_at, created_at::date) >= CURRENT_DATE - 183 AND status NOT IN ('REJECTED','ACCEPTED','WITHDRAWN')) AS last6m, " +
            "  COUNT(*) FILTER (WHERE COALESCE(applied_at, created_at::date) >= CURRENT_DATE - 274 AND status NOT IN ('REJECTED','ACCEPTED','WITHDRAWN')) AS last9m, " +
            "  COUNT(*) FILTER (WHERE COALESCE(applied_at, created_at::date) >= CURRENT_DATE - 365 AND status NOT IN ('REJECTED','ACCEPTED','WITHDRAWN')) AS last1y " +
            "FROM applications_snapshot WHERE user_id=? AND deleted_at IS NULL";

        long[] result = {0, 0, 0, 0, 0, 0, 0};
        jdbc.query(sql, (RowCallbackHandler) rs -> {
            result[0] = rs.getLong(1);
            result[1] = rs.getLong(2);
            result[2] = rs.getLong(3);
            result[3] = rs.getLong(4);
            result[4] = rs.getLong(5);
            result[5] = rs.getLong(6);
            result[6] = rs.getLong(7);
        }, DEMO_USER_ID);
        return new OpenWindowsDto(result[0], result[1], result[2], result[3], result[4], result[5], result[6]);
    }
}
