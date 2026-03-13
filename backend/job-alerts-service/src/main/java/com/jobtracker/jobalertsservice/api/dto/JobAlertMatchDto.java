package com.jobtracker.jobalertsservice.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JobAlertMatchDto(
        UUID id,
        UUID alertId,
        String platform,
        String title,
        String jobUrl,
        String companyName,
        String location,
        OffsetDateTime postedAt,
        OffsetDateTime seenAt,
        OffsetDateTime createdAt
) {}
