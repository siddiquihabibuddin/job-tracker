package com.jobtracker.ai.api.dto;

import java.util.List;

public record InsightsDto(List<String> insights, String generatedAt) {}
