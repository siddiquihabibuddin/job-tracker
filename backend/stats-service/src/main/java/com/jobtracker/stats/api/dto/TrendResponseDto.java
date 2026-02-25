package com.jobtracker.stats.api.dto;

import java.util.List;

public record TrendResponseDto(
        String period,
        List<TrendPointDto> points
) {}
