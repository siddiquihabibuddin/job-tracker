package com.jobtracker.stats.api.dto;

import java.util.UUID;

public record StaleAppDto(
    UUID appId,
    String company,
    String role,
    String status,
    int daysSinceLastEvent,
    String flaggedAt,
    String appliedAt) {}
