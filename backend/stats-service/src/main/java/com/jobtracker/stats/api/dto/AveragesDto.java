package com.jobtracker.stats.api.dto;

import java.util.List;

public record AveragesDto(
        String groupBy,
        Integer year,
        String periodStart,
        String periodEnd,
        long total,
        double dailyAverage,
        double weeklyAverage,
        double monthlyAverage,
        List<AveragePeriodDto> periods,
        String generatedAt
) {}
