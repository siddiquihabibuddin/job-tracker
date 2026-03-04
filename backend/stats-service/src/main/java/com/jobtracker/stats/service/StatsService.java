package com.jobtracker.stats.service;

import com.jobtracker.stats.api.dto.BreakdownResponseDto;
import com.jobtracker.stats.api.dto.BreakdownRowDto;
import com.jobtracker.stats.api.dto.OpenWindowsDto;
import com.jobtracker.stats.api.dto.StatsSummaryDto;
import com.jobtracker.stats.api.dto.TrendPointDto;
import com.jobtracker.stats.api.dto.TrendResponseDto;
import com.jobtracker.stats.config.CacheConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import java.sql.Date;
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

    @Cacheable(cacheNames = CacheConfig.CACHE_SUMMARY, key = "#windowDays")
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

    @Cacheable(cacheNames = CacheConfig.CACHE_TREND, key = "#weeks")
    public TrendResponseDto getTrend(int weeks) {
        log.info("Serving trend query weeks={}", weeks);
        queriesCounter.increment();

        LocalDate currentWeekMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate windowStart = currentWeekMonday.minusWeeks(weeks - 1);

        Map<LocalDate, Long> dbResults = new LinkedHashMap<>();
        jdbc.query(
                "SELECT week_start, cnt FROM agg_weekly WHERE user_id=? AND week_start >= ? ORDER BY week_start",
                (RowCallbackHandler) rs -> {
                    LocalDate weekStart = rs.getObject(1, LocalDate.class);
                    dbResults.put(weekStart, rs.getLong(2));
                },
                DEMO_USER_ID, Date.valueOf(windowStart));

        List<TrendPointDto> points = new ArrayList<>(weeks);
        for (int i = weeks - 1; i >= 0; i--) {
            LocalDate weekStart = currentWeekMonday.minusWeeks(i);
            LocalDate weekEnd = weekStart.plusDays(6);
            long count = dbResults.getOrDefault(weekStart, 0L);
            points.add(new TrendPointDto(weekStart.format(DATE_FMT), weekEnd.format(DATE_FMT), count));
        }

        return new TrendResponseDto("week", points);
    }

    @Cacheable(cacheNames = CacheConfig.CACHE_BREAKDOWN, key = "#groupBy + ':' + #year")
    public BreakdownResponseDto getBreakdown(String groupBy, Integer year) {
        log.info("Serving breakdown query groupBy={} year={}", groupBy, year);
        queriesCounter.increment();

        boolean byMonth = "month".equalsIgnoreCase(groupBy);
        String[] MONTH_LABELS = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};

        List<BreakdownRowDto> rows;

        if (byMonth) {
            int targetYear = (year != null) ? year : LocalDate.now().getYear();

            // month (1-12) → [totalApplied, totalRejected, totalOpen]
            Map<Integer, long[]> monthMap = new LinkedHashMap<>();
            jdbc.query(
                    "SELECT month, status, cnt FROM agg_monthly WHERE user_id=? AND year=? ORDER BY month",
                    (RowCallbackHandler) rs -> {
                        int m = rs.getInt(1);
                        String status = rs.getString(2);
                        long cnt = rs.getLong(3);
                        long[] totals = monthMap.computeIfAbsent(m, k -> new long[3]);
                        totals[0] += cnt;
                        if ("REJECTED".equals(status)) totals[1] += cnt;
                        if (!"REJECTED".equals(status) && !"ACCEPTED".equals(status) && !"WITHDRAWN".equals(status)) totals[2] += cnt;
                    },
                    DEMO_USER_ID, targetYear);

            rows = new ArrayList<>(12);
            for (int m = 1; m <= 12; m++) {
                long[] t = monthMap.getOrDefault(m, new long[3]);
                rows.add(new BreakdownRowDto(MONTH_LABELS[m - 1], m, t[0], t[1], t[2]));
            }

            OpenWindowsDto windows = queryOpenWindows();
            return new BreakdownResponseDto("month", targetYear, rows, windows);

        } else {
            // year → [totalApplied, totalRejected, totalOpen]
            Map<Integer, long[]> yearMap = new LinkedHashMap<>();
            jdbc.query(
                    "SELECT year, status, cnt FROM agg_monthly WHERE user_id=? ORDER BY year",
                    (RowCallbackHandler) rs -> {
                        int y = rs.getInt(1);
                        String status = rs.getString(2);
                        long cnt = rs.getLong(3);
                        long[] totals = yearMap.computeIfAbsent(y, k -> new long[3]);
                        totals[0] += cnt;
                        if ("REJECTED".equals(status)) totals[1] += cnt;
                        if (!"REJECTED".equals(status) && !"ACCEPTED".equals(status) && !"WITHDRAWN".equals(status)) totals[2] += cnt;
                    },
                    DEMO_USER_ID);

            rows = new ArrayList<>();
            for (Map.Entry<Integer, long[]> entry : yearMap.entrySet()) {
                int y = entry.getKey();
                long[] t = entry.getValue();
                rows.add(new BreakdownRowDto(String.valueOf(y), y, t[0], t[1], t[2]));
            }

            OpenWindowsDto windows = queryOpenWindows();
            return new BreakdownResponseDto("year", null, rows, windows);
        }
    }

    private OpenWindowsDto queryOpenWindows() {
        // 7d/15d/30d: day precision — query snapshot directly
        String snapSql =
            "SELECT " +
            "  COUNT(*) FILTER (WHERE COALESCE(applied_at, created_at::date) >= CURRENT_DATE - 7   AND status NOT IN ('REJECTED','ACCEPTED','WITHDRAWN')) AS last7d, " +
            "  COUNT(*) FILTER (WHERE COALESCE(applied_at, created_at::date) >= CURRENT_DATE - 15  AND status NOT IN ('REJECTED','ACCEPTED','WITHDRAWN')) AS last15d, " +
            "  COUNT(*) FILTER (WHERE COALESCE(applied_at, created_at::date) >= CURRENT_DATE - 30  AND status NOT IN ('REJECTED','ACCEPTED','WITHDRAWN')) AS last30d " +
            "FROM applications_snapshot WHERE user_id=? AND deleted_at IS NULL";

        long[] snapResult = {0, 0, 0};
        jdbc.query(snapSql, (RowCallbackHandler) rs -> {
            snapResult[0] = rs.getLong(1);
            snapResult[1] = rs.getLong(2);
            snapResult[2] = rs.getLong(3);
        }, DEMO_USER_ID);

        // 3m/6m/9m/1y: month precision — query agg_monthly
        LocalDate today = LocalDate.now();
        LocalDate cutoff3m = today.minusDays(92);
        LocalDate cutoff6m = today.minusDays(183);
        LocalDate cutoff9m = today.minusDays(274);
        LocalDate cutoff1y = today.minusDays(365);

        int minYear  = cutoff1y.getYear();
        int minMonth = cutoff1y.getMonthValue();

        long[] aggResult = {0, 0, 0, 0};
        jdbc.query("""
                SELECT year, month, SUM(cnt) FROM agg_monthly
                WHERE user_id=? AND status NOT IN ('REJECTED','ACCEPTED','WITHDRAWN')
                  AND (year > ? OR (year = ? AND month >= ?))
                GROUP BY year, month""",
                (RowCallbackHandler) rs -> {
                    int y = rs.getInt(1);
                    int m = rs.getInt(2);
                    long cnt = rs.getLong(3);
                    int periodYM = y * 12 + m;
                    if (periodYM >= cutoff3m.getYear() * 12 + cutoff3m.getMonthValue()) aggResult[0] += cnt;
                    if (periodYM >= cutoff6m.getYear() * 12 + cutoff6m.getMonthValue()) aggResult[1] += cnt;
                    if (periodYM >= cutoff9m.getYear() * 12 + cutoff9m.getMonthValue()) aggResult[2] += cnt;
                    aggResult[3] += cnt; // all rows already satisfy cutoff1y from WHERE clause
                },
                DEMO_USER_ID, minYear, minYear, minMonth);

        return new OpenWindowsDto(snapResult[0], snapResult[1], snapResult[2],
                                  aggResult[0], aggResult[1], aggResult[2], aggResult[3]);
    }
}
