package com.jobtracker.stats.api.dto;

public record AveragesDto(
        String groupBy,
        Integer year,
        String periodStart,
        String periodEnd,
        long total,
        double dailyAverage,
        double weeklyAverage,
        double monthlyAverage,
        String generatedAt
) {}
