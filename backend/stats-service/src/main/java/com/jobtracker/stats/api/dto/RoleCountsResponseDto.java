package com.jobtracker.stats.api.dto;

import java.util.List;

public record RoleCountsResponseDto(
        String groupBy,
        Integer year,
        List<RoleCountRowDto> rows
) {}
