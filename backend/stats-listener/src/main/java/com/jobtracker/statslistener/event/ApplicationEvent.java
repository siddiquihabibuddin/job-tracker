package com.jobtracker.statslistener.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApplicationEvent(
        String eventType,
        UUID id,
        UUID userId,
        String status,
        String source,
        String role,
        String company,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt,
        OffsetDateTime occurredAt,
        java.time.LocalDate appliedAt,
        Boolean gotCall) {
}
