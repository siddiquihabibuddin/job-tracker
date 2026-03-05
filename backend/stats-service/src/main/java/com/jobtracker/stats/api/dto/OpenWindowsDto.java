package com.jobtracker.stats.api.dto;

public record OpenWindowsDto(long today, long last7d, long last15d, long last30d, long last3m, long last6m, long last9m, long last1y) {}
