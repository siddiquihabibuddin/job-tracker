package com.jobtracker.apps.api.dto;

import java.util.List;

public record CsvImportResult(int imported, int updated, int failed, List<String> errors) {}
