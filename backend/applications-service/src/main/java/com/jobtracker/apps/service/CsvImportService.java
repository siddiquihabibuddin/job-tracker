package com.jobtracker.apps.service;

import com.jobtracker.apps.api.dto.CreateApplicationRequest;
import com.jobtracker.apps.api.dto.CsvImportResult;
import com.jobtracker.apps.domain.model.AppStatus;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CsvImportService {

    private static final Map<String, AppStatus> STATUS_MAP = Map.ofEntries(
            Map.entry("applied",       AppStatus.APPLIED),
            Map.entry("open",          AppStatus.APPLIED),
            Map.entry("phone",         AppStatus.PHONE),
            Map.entry("phone screen",  AppStatus.PHONE),
            Map.entry("onsite",        AppStatus.ONSITE),
            Map.entry("on-site",       AppStatus.ONSITE),
            Map.entry("offer",         AppStatus.OFFER),
            Map.entry("rejected",      AppStatus.REJECTED),
            Map.entry("closed",        AppStatus.REJECTED),
            Map.entry("accepted",      AppStatus.ACCEPTED),
            Map.entry("withdrawn",     AppStatus.WITHDRAWN)
    );

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yy"),       // e.g. 3/3/26
            DateTimeFormatter.ofPattern("MM/dd/yy"),     // e.g. 03/03/26
            DateTimeFormatter.ofPattern("MM-dd-yyyy"),
            DateTimeFormatter.ofPattern("d-MMM-yyyy"),   // e.g. 3-Mar-2026
            DateTimeFormatter.ofPattern("d MMM yyyy"),   // e.g. 3 Mar 2026
            DateTimeFormatter.ofPattern("MMM d, yyyy"),  // e.g. Mar 3, 2026
            DateTimeFormatter.ofPattern("yyyy/MM/dd")    // e.g. 2026/03/03
    );

    private final ApplicationService applicationService;

    public CsvImportService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public CsvImportResult importCsv(MultipartFile file) throws IOException {
        List<String[]> rows;
        try (var reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            rows = reader.readAll();
        } catch (CsvException e) {
            throw new IOException("Failed to parse CSV: " + e.getMessage(), e);
        }

        if (rows.isEmpty()) return new CsvImportResult(0, 0, List.of());

        // Skip header row (index 0); process from row 1 onwards
        int imported = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 1; i < rows.size(); i++) {
            String[] cols = rows.get(i);
            int rowNum = i + 1;

            // Skip blank rows
            if (isBlankRow(cols)) continue;

            try {
                String company        = cell(cols, 0);
                String role           = cell(cols, 1);
                String location       = cell(cols, 2);
                String salaryRange    = cell(cols, 3);
                String applyDateStr   = cell(cols, 4);
                String finalStatus    = cell(cols, 5);
                String jobLink        = cell(cols, 6);
                String resumeUploaded = cell(cols, 7);
                String callStr        = cell(cols, 8);
                String rejectDateStr  = cell(cols, 9);
                String loginDetails   = cell(cols, 10);
                // col 11 = Days pending — intentionally skipped

                if (company.isBlank()) {
                    errors.add("row " + rowNum + ": company is blank");
                    failed++;
                    continue;
                }
                if (role.isBlank()) {
                    errors.add("row " + rowNum + ": role is blank");
                    failed++;
                    continue;
                }

                BigDecimal[] salary  = parseSalary(salaryRange);
                boolean gotCall      = parseBoolean(callStr);
                AppStatus status     = parseStatus(finalStatus, gotCall);
                LocalDate appliedAt  = parseDate(applyDateStr);
                LocalDate rejectDate = parseDate(rejectDateStr);

                var req = new CreateApplicationRequest(
                        company,
                        role,
                        status,
                        null,                                           // source
                        location.isBlank() ? null : location,
                        salary[0],
                        salary[1],
                        "USD",                                          // currency
                        null,                                           // nextFollowUpOn
                        null,                                           // tags
                        null,                                           // notes
                        appliedAt,
                        jobLink.isBlank() ? null : jobLink,
                        resumeUploaded.isBlank() ? null : resumeUploaded,
                        gotCall,
                        rejectDate,
                        loginDetails.isBlank() ? null : loginDetails
                );

                applicationService.create(req);
                imported++;
            } catch (Exception e) {
                errors.add("row " + rowNum + ": " + e.getMessage());
                failed++;
            }
        }

        return new CsvImportResult(imported, failed, errors);
    }

    // --- helpers ---

    private boolean isBlankRow(String[] cols) {
        for (String c : cols) {
            if (c != null && !c.isBlank()) return false;
        }
        return true;
    }

    private String cell(String[] cols, int idx) {
        if (idx >= cols.length) return "";
        return cols[idx] == null ? "" : cols[idx].trim();
    }

    private BigDecimal[] parseSalary(String raw) {
        if (raw == null || raw.isBlank()) return new BigDecimal[]{null, null};
        // Remove $, commas; expand K/k suffix to three zeros
        String cleaned = raw.replaceAll("[$,]", "").trim();
        cleaned = cleaned.replaceAll("(?i)k", "000");
        // Split on dash or en-dash with optional surrounding spaces
        String[] parts = cleaned.split("\\s*[-\u2013]\\s*");
        try {
            if (parts.length == 2) {
                return new BigDecimal[]{
                        new BigDecimal(parts[0].trim()),
                        new BigDecimal(parts[1].trim())
                };
            } else if (parts.length == 1 && !parts[0].isBlank()) {
                return new BigDecimal[]{new BigDecimal(parts[0].trim()), null};
            }
        } catch (NumberFormatException ignored) {
            // fall through to null,null
        }
        return new BigDecimal[]{null, null};
    }

    private AppStatus parseStatus(String raw, boolean gotCall) {
        if (raw == null || raw.isBlank()) return AppStatus.APPLIED;
        String key = raw.toLowerCase().trim();
        AppStatus status = STATUS_MAP.getOrDefault(key, AppStatus.APPLIED);
        // Open + got a call → promote to PHONE
        if (gotCall && "open".equals(key)) return AppStatus.PHONE;
        return status;
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        for (var fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw.trim(), fmt);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        return null;
    }

    private boolean parseBoolean(String raw) {
        if (raw == null) return false;
        String v = raw.trim().toLowerCase();
        return v.equals("yes") || v.equals("true") || v.equals("1");
    }
}
