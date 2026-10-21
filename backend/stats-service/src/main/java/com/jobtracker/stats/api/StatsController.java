package com.jobtracker.stats.api;

import com.jobtracker.stats.api.dto.ActivityItemDto;
import com.jobtracker.stats.api.dto.AveragesDto;
import com.jobtracker.stats.api.dto.BreakdownResponseDto;
import com.jobtracker.stats.api.dto.CompanyCountDto;
import com.jobtracker.stats.api.dto.StaleAppDto;
import com.jobtracker.stats.api.dto.RoleCountsResponseDto;
import com.jobtracker.stats.api.dto.StatsSummaryDto;
import com.jobtracker.stats.api.dto.TrendResponseDto;
import com.jobtracker.stats.service.StatsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/stats")
@Validated
public class StatsController {

    private final StatsService svc;

    public StatsController(StatsService svc) {
        this.svc = svc;
    }

    @GetMapping("/summary")
    public StatsSummaryDto summary(@RequestParam(defaultValue = "7d") String window) {
        int days = parseWindowDays(window);
        if (days < 1 || days > 365) {
            throw new IllegalArgumentException("window must be between 1d and 365d");
        }
        return svc.getSummary(currentUserId(), days);
    }

    @GetMapping("/trend")
    public TrendResponseDto trend(
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(defaultValue = "12") @Min(1) @Max(52) int weeks) {
        return svc.getTrend(currentUserId(), weeks);
    }

    @GetMapping("/averages")
    public AveragesDto averages(
            @RequestParam(defaultValue = "month") String groupBy,
            @RequestParam(required = false) Integer year) {
        return svc.getAverages(currentUserId(), groupBy, year);
    }

    @GetMapping("/breakdown")
    public BreakdownResponseDto breakdown(
            @RequestParam(defaultValue = "month") String groupBy,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String tz) {
        return svc.getBreakdown(currentUserId(), groupBy, year, tz);
    }

    @GetMapping("/roles")
    public RoleCountsResponseDto roles(
            @RequestParam(defaultValue = "month") String groupBy,
            @RequestParam(required = false) Integer year) {
        return svc.getRoleCounts(currentUserId(), groupBy, year);
    }

    @GetMapping("/activity/{appId}")
    public List<ActivityItemDto> activityFeed(@PathVariable UUID appId) {
        return svc.getActivityFeed(currentUserId(), appId);
    }

    @GetMapping("/companies")
    public List<CompanyCountDto> topCompanies() {
        return svc.getTopCompanies(currentUserId());
    }

    @GetMapping("/stale")
    public List<StaleAppDto> stale() {
        return svc.getStaleApplications(currentUserId());
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String sub = jwt.getSubject();
            if (sub != null) return UUID.fromString(sub);
        }
        throw new IllegalStateException("No authenticated user");
    }

    private int parseWindowDays(String window) {
        if (window.endsWith("d")) {
            return Integer.parseInt(window.substring(0, window.length() - 1));
        }
        return Integer.parseInt(window);
    }
}
