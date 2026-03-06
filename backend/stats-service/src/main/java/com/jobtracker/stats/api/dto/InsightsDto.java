package com.jobtracker.stats.api.dto;

import java.util.List;

public record InsightsDto(List<String> insights, String generatedAt) {}
