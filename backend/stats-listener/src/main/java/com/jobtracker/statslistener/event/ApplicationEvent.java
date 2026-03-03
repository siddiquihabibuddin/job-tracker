package com.jobtracker.statslistener.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApplicationEvent(
        String eventType,
        UUID id,
        UUID userId,
        String status,
        String source,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt,
        OffsetDateTime occurredAt,
        java.time.LocalDate appliedAt) {
}
