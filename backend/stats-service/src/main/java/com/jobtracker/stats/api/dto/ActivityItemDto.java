package com.jobtracker.stats.api.dto;

import java.util.UUID;

public record ActivityItemDto(UUID id, String eventType, String message, String occurredAt) {}
