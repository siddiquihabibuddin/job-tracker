package com.jobtracker.stats.api;

import com.jobtracker.stats.api.dto.StatsSummaryDto;
import com.jobtracker.stats.api.dto.TrendResponseDto;
import com.jobtracker.stats.service.StatsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        return svc.getSummary(days);
    }

    @GetMapping("/trend")
    public TrendResponseDto trend(
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(defaultValue = "12") @Min(1) @Max(52) int weeks) {
        return svc.getTrend(weeks);
    }

    private int parseWindowDays(String window) {
        if (window.endsWith("d")) {
            return Integer.parseInt(window.substring(0, window.length() - 1));
        }
        return Integer.parseInt(window);
    }
}
