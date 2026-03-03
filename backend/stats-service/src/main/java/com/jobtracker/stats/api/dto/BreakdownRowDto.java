package com.jobtracker.stats.api.dto;

public record BreakdownRowDto(
        String label,
        int periodNum,
        long totalApplied,
        long totalRejected,
        long totalOpen
) {}
