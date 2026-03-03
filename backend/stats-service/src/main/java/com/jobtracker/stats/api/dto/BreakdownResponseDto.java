package com.jobtracker.stats.api.dto;

import java.util.List;

public record BreakdownResponseDto(
        String groupBy,
        Integer year,
        List<BreakdownRowDto> rows,
        OpenWindowsDto openWindows
) {}
