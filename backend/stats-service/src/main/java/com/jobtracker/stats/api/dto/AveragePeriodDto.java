package com.jobtracker.stats.api.dto;

/** Averages for one calendar month (month view) or one calendar year (year view). */
public record AveragePeriodDto(
        String label,
        int periodNum,
        long total,
        double dailyAverage,
        double weeklyAverage,
        double monthlyAverage,
        boolean partial,
        boolean future
) {}
