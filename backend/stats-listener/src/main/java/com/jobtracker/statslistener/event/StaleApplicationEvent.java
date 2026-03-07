package com.jobtracker.statslistener.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StaleApplicationEvent(
    UUID appId,
    UUID userId,
    String company,
    String role,
    String status,
    int daysSinceLastEvent,
    OffsetDateTime detectedAt) {}
