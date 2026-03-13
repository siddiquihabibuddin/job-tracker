package com.jobtracker.jobalertsservice.api.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record JobAlertDto(
        UUID id,
        UUID userId,
        List<AlertCompanyDto> companies,
        String roleKeywords,
        List<String> platforms,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
