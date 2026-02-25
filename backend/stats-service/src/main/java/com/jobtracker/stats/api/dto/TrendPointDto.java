package com.jobtracker.stats.api.dto;

public record TrendPointDto(
        String start,
        String end,
        long count
) {}
