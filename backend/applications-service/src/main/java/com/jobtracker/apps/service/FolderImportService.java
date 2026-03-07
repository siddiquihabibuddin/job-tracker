package com.jobtracker.apps.service;

import com.jobtracker.apps.api.dto.FolderImportResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Service
public class FolderImportService {

    private static final Logger log = LoggerFactory.getLogger(FolderImportService.class);

    @Value("${csv.import.folder:/app/csv-imports}")
    private String importFolder;

    private final CsvImportService csvImportService;

    public FolderImportService(CsvImportService csvImportService) {
        this.csvImportService = csvImportService;
    }

    public FolderImportResult importFromFolder() throws IOException {
        Path folderPath = Path.of(importFolder);
        if (!Files.exists(folderPath)) {
            Files.createDirectories(folderPath);
        }
        Path processedPath = folderPath.resolve("processed");
        Files.createDirectories(processedPath);

        File[] csvFiles = folderPath.toFile().listFiles(
                (dir, name) -> name.toLowerCase().endsWith(".csv"));

        if (csvFiles == null || csvFiles.length == 0) {
            return new FolderImportResult(0, 0, 0, List.of());
        }

        int totalImported = 0, totalFailed = 0;
        List<FolderImportResult.FileImportSummary> summaries = new ArrayList<>();

        for (File file : csvFiles) {
            log.info("Processing CSV file: {}", file.getName());
            var result = csvImportService.importCsv(file);
            summaries.add(new FolderImportResult.FileImportSummary(
                    file.getName(), result.imported(), result.failed(), result.errors()));
            totalImported += result.imported();
            totalFailed += result.failed();
            Files.move(file.toPath(), processedPath.resolve(file.getName()),
                    StandardCopyOption.REPLACE_EXISTING);
            log.info("Moved {} to processed/ ({} imported, {} failed)",
                    file.getName(), result.imported(), result.failed());
        }

        return new FolderImportResult(csvFiles.length, totalImported, totalFailed, summaries);
    }
}
