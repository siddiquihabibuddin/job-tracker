package com.jobtracker.apps.api.dto;

import java.util.List;

public record PageResponse<T>(List<T> items, int limit, long totalElements, int totalPages, int page) {}