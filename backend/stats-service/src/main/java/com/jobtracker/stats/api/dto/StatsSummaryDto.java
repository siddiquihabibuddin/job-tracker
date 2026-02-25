package com.jobtracker.stats.api.dto;

import java.util.Map;

public record StatsSummaryDto(
        int windowDays,
        long totalApplied,
        Map<String, Long> byStatus,
        Map<String, Long> bySource,
        String generatedAt
) {}
