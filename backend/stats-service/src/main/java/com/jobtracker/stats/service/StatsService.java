package com.jobtracker.stats.service;

import com.jobtracker.stats.api.dto.ActivityItemDto;
import com.jobtracker.stats.api.dto.BreakdownResponseDto;
import com.jobtracker.stats.api.dto.CompanyCountDto;
import com.jobtracker.stats.api.dto.StaleAppDto;
import com.jobtracker.stats.api.dto.BreakdownRowDto;
import com.jobtracker.stats.api.dto.OpenWindowsDto;
import com.jobtracker.stats.api.dto.RoleCountRowDto;
import com.jobtracker.stats.api.dto.RoleCountsResponseDto;
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
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final JdbcTemplate jdbc;
    private final Counter queriesCounter;

    public StatsService(JdbcTemplate jdbc, MeterRegistry meterRegistry) {
        this.jdbc = jdbc;
        this.queriesCounter = Counter.builder("stats.queries.total")
                .description("Total stats queries served")
                .register(meterRegistry);
    }

    @Cacheable(cacheNames = CacheConfig.CACHE_SUMMARY, key = "#userId + ':' + #windowDays")
    public StatsSummaryDto getSummary(UUID userId, int windowDays) {
        log.info("Serving summary query userId={} windowDays={}", userId, windowDays);
        queriesCounter.increment();
        String interval = windowDays + " days";

        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM applications_snapshot WHERE user_id=? AND deleted_at IS NULL AND COALESCE(applied_at, created_at::date) >= (NOW() - ?::interval)::date",
                Long.class, userId, interval);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        jdbc.query(
                "SELECT status, COUNT(*) FROM applications_snapshot WHERE user_id=? AND deleted_at IS NULL AND COALESCE(applied_at, created_at::date) >= (NOW() - ?::interval)::date GROUP BY status",
                (RowCallbackHandler) rs -> byStatus.put(rs.getString(1), rs.getLong(2)),
                userId, interval);

        Map<String, Long> bySource = new LinkedHashMap<>();
        jdbc.query(
                "SELECT COALESCE(source, 'Unknown'), COUNT(*) FROM applications_snapshot WHERE user_id=? AND deleted_at IS NULL AND COALESCE(applied_at, created_at::date) >= (NOW() - ?::interval)::date GROUP BY 1",
                (RowCallbackHandler) rs -> bySource.put(rs.getString(1), rs.getLong(2)),
                userId, interval);

        return new StatsSummaryDto(
                windowDays,
                total != null ? total : 0L,
                byStatus,
                bySource,
                OffsetDateTime.now().toString());
    }

    @Cacheable(cacheNames = CacheConfig.CACHE_TREND, key = "#userId + ':' + #weeks")
    public TrendResponseDto getTrend(UUID userId, int weeks) {
        log.info("Serving trend query userId={} weeks={}", userId, weeks);
        queriesCounter.increment();

        LocalDate currentWeekMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate windowStart = currentWeekMonday.minusWeeks(weeks - 1);

        Map<LocalDate, long[]> dbResults = new LinkedHashMap<>();
        jdbc.query(
                "SELECT week_start, cnt, calls_cnt FROM agg_weekly WHERE user_id=? AND week_start >= ? ORDER BY week_start",
                (RowCallbackHandler) rs -> {
                    LocalDate weekStart = rs.getObject(1, LocalDate.class);
                    dbResults.put(weekStart, new long[]{rs.getLong(2), rs.getLong(3)});
                },
                userId, Date.valueOf(windowStart));

        List<TrendPointDto> points = new ArrayList<>(weeks);
        for (int i = weeks - 1; i >= 0; i--) {
            LocalDate weekStart = currentWeekMonday.minusWeeks(i);
            LocalDate weekEnd = weekStart.plusDays(6);
            long[] row = dbResults.getOrDefault(weekStart, new long[]{0L, 0L});
            points.add(new TrendPointDto(weekStart.format(DATE_FMT), weekEnd.format(DATE_FMT), row[0], row[1]));
        }

        return new TrendResponseDto("week", points);
    }

    @Cacheable(cacheNames = CacheConfig.CACHE_BREAKDOWN, key = "#userId + ':' + #groupBy + ':' + #year + ':' + #tz")
    public BreakdownResponseDto getBreakdown(UUID userId, String groupBy, Integer year, String tz) {
        log.info("Serving breakdown query userId={} groupBy={} year={} tz={}", userId, groupBy, year, tz);
        queriesCounter.increment();

        boolean byMonth = "month".equalsIgnoreCase(groupBy);
        String[] MONTH_LABELS = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};

        List<BreakdownRowDto> rows;

        if (byMonth) {
            int targetYear = (year != null) ? year : LocalDate.now().getYear();

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
                    userId, targetYear);

            rows = new ArrayList<>(12);
            for (int m = 1; m <= 12; m++) {
                long[] t = monthMap.getOrDefault(m, new long[3]);
                rows.add(new BreakdownRowDto(MONTH_LABELS[m - 1], m, t[0], t[1], t[2]));
            }

            OpenWindowsDto windows = queryOpenWindows(userId, tz);
            return new BreakdownResponseDto("month", targetYear, rows, windows);

        } else {
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
                    userId);

            rows = new ArrayList<>();
            for (Map.Entry<Integer, long[]> entry : yearMap.entrySet()) {
                int y = entry.getKey();
                long[] t = entry.getValue();
                rows.add(new BreakdownRowDto(String.valueOf(y), y, t[0], t[1], t[2]));
            }

            OpenWindowsDto windows = queryOpenWindows(userId, tz);
            return new BreakdownResponseDto("year", null, rows, windows);
        }
    }

    @Cacheable(cacheNames = CacheConfig.CACHE_ROLES, key = "#userId + ':' + #groupBy + ':' + #year")
    public RoleCountsResponseDto getRoleCounts(UUID userId, String groupBy, Integer year) {
        log.info("Serving role counts query userId={} groupBy={} year={}", userId, groupBy, year);
        queriesCounter.increment();

        boolean byMonth = "month".equalsIgnoreCase(groupBy);
        String[] MONTH_LABELS = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};

        List<RoleCountRowDto> rows;

        if (byMonth) {
            int targetYear = (year != null) ? year : LocalDate.now().getYear();

            Map<Integer, long[]> monthMap = new LinkedHashMap<>();
            jdbc.query(
                    "SELECT month, category, cnt FROM agg_role WHERE user_id=? AND year=? ORDER BY month",
                    (RowCallbackHandler) rs -> {
                        int m = rs.getInt(1);
                        String category = rs.getString(2);
                        long cnt = rs.getLong(3);
                        long[] totals = monthMap.computeIfAbsent(m, k -> new long[4]);
                        switch (category) {
                            case "ENGINEER_DEV" -> totals[0] += cnt;
                            case "MANAGER"      -> totals[1] += cnt;
                            case "ARCHITECT"    -> totals[2] += cnt;
                            default             -> totals[3] += cnt;
                        }
                    },
                    userId, targetYear);

            rows = new ArrayList<>(12);
            for (int m = 1; m <= 12; m++) {
                long[] t = monthMap.getOrDefault(m, new long[4]);
                rows.add(new RoleCountRowDto(MONTH_LABELS[m - 1], m, t[0], t[1], t[2], t[3]));
            }
            return new RoleCountsResponseDto("month", targetYear, rows);

        } else {
            Map<Integer, long[]> yearMap = new LinkedHashMap<>();
            jdbc.query(
                    "SELECT year, category, cnt FROM agg_role WHERE user_id=? ORDER BY year",
                    (RowCallbackHandler) rs -> {
                        int y = rs.getInt(1);
                        String category = rs.getString(2);
                        long cnt = rs.getLong(3);
                        long[] totals = yearMap.computeIfAbsent(y, k -> new long[4]);
                        switch (category) {
                            case "ENGINEER_DEV" -> totals[0] += cnt;
                            case "MANAGER"      -> totals[1] += cnt;
                            case "ARCHITECT"    -> totals[2] += cnt;
                            default             -> totals[3] += cnt;
                        }
                    },
                    userId);

            rows = new ArrayList<>();
            for (Map.Entry<Integer, long[]> entry : yearMap.entrySet()) {
                int y = entry.getKey();
                long[] t = entry.getValue();
                rows.add(new RoleCountRowDto(String.valueOf(y), y, t[0], t[1], t[2], t[3]));
            }
            return new RoleCountsResponseDto("year", null, rows);
        }
    }

    @Cacheable(cacheNames = CacheConfig.CACHE_COMPANIES, key = "#userId")
    public List<CompanyCountDto> getTopCompanies(UUID userId) {
        log.info("Serving top-companies query userId={}", userId);
        queriesCounter.increment();
        List<CompanyCountDto> result = new ArrayList<>();
        jdbc.query(
            "SELECT company, COUNT(*) AS cnt, MAX(COALESCE(applied_at, created_at::date))::text AS last_applied " +
            "FROM applications_snapshot " +
            "WHERE user_id = ? AND deleted_at IS NULL AND company IS NOT NULL " +
            "GROUP BY company ORDER BY cnt DESC, company ASC LIMIT 10",
            (RowCallbackHandler) rs -> result.add(new CompanyCountDto(rs.getString(1), rs.getLong(2), rs.getString(3))),
            userId);
        return result;
    }

    public List<StaleAppDto> getStaleApplications(UUID userId) {
        List<StaleAppDto> result = new ArrayList<>();
        jdbc.query(
            "SELECT sf.app_id, sf.company, sf.role, sf.status, sf.days_stale, sf.flagged_at, snap.applied_at " +
            "FROM stale_flags sf " +
            "LEFT JOIN applications_snapshot snap ON snap.id = sf.app_id " +
            "WHERE sf.user_id = ? AND sf.resolved_at IS NULL " +
            "ORDER BY snap.applied_at DESC NULLS LAST",
            (RowCallbackHandler) rs -> {
                var appliedAt = rs.getDate("applied_at");
                result.add(new StaleAppDto(
                    UUID.fromString(rs.getString("app_id")),
                    rs.getString("company"),
                    rs.getString("role"),
                    rs.getString("status"),
                    rs.getInt("days_stale"),
                    rs.getObject("flagged_at", OffsetDateTime.class).toString(),
                    appliedAt != null ? appliedAt.toLocalDate().toString() : null));
            },
            userId);
        return result;
    }

    public List<ActivityItemDto> getActivityFeed(UUID userId, UUID appId) {
        List<ActivityItemDto> items = new ArrayList<>();
        jdbc.query(
            "SELECT id, event_type, message, occurred_at FROM activity_feed WHERE app_id = ? AND user_id = ? ORDER BY occurred_at DESC LIMIT 50",
            (RowCallbackHandler) rs -> items.add(new ActivityItemDto(
                UUID.fromString(rs.getString(1)),
                rs.getString(2),
                rs.getString(3),
                rs.getObject(4, java.time.OffsetDateTime.class).toString()
            )),
            appId, userId);
        return items;
    }

    private OpenWindowsDto queryOpenWindows(UUID userId, String tz) {
        String clientToday = (tz != null && !tz.isBlank())
            ? "(NOW() AT TIME ZONE '" + tz.replace("'", "") + "')::date"
            : "CURRENT_DATE";
        String snapSql =
            "SELECT " +
            "  COUNT(*) FILTER (WHERE COALESCE(applied_at, created_at::date) = " + clientToday + " AND status NOT IN ('REJECTED','ACCEPTED','WITHDRAWN')) AS today, " +
            "  COUNT(*) FILTER (WHERE COALESCE(applied_at, created_at::date) >= " + clientToday + " - 7   AND status NOT IN ('REJECTED','ACCEPTED','WITHDRAWN')) AS last7d, " +
            "  COUNT(*) FILTER (WHERE COALESCE(applied_at, created_at::date) >= " + clientToday + " - 15  AND status NOT IN ('REJECTED','ACCEPTED','WITHDRAWN')) AS last15d, " +
            "  COUNT(*) FILTER (WHERE COALESCE(applied_at, created_at::date) >= " + clientToday + " - 30  AND status NOT IN ('REJECTED','ACCEPTED','WITHDRAWN')) AS last30d " +
            "FROM applications_snapshot WHERE user_id=? AND deleted_at IS NULL";

        long[] snapResult = {0, 0, 0, 0};
        jdbc.query(snapSql, (RowCallbackHandler) rs -> {
            snapResult[0] = rs.getLong(1);
            snapResult[1] = rs.getLong(2);
            snapResult[2] = rs.getLong(3);
            snapResult[3] = rs.getLong(4);
        }, userId);

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
                    aggResult[3] += cnt;
                },
                userId, minYear, minYear, minMonth);

        return new OpenWindowsDto(snapResult[0], snapResult[1], snapResult[2], snapResult[3],
                                  aggResult[0], aggResult[1], aggResult[2], aggResult[3]);
    }
}
