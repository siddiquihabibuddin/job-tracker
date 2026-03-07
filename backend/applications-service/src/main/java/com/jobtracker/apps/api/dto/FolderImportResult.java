package com.jobtracker.apps.api.dto;

import java.util.List;

public record FolderImportResult(
        int totalFiles,
        int totalImported,
        int totalFailed,
        List<FileImportSummary> files) {

    public record FileImportSummary(
            String fileName,
            int imported,
            int failed,
            List<String> errors) {}
}
