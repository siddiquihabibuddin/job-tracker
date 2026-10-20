package com.jobtracker.stats.api.dto;

public record AveragesDto(
        double dailyAverage,
        double weeklyAverage,
        double monthlyAverage,
        int weeks,
        int months,
        String generatedAt
) {}
