package com.jobtracker.apps.api.dto;

import com.jobtracker.apps.domain.model.AppStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record ApplicationDto(
        UUID id,
        String company,
        String role,
        AppStatus status,
        String source,
        String location,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String currency,
        LocalDate nextFollowUpOn,
        String[] tags,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}