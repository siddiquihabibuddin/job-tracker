package com.jobtracker.ai.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ApplicationContextDto(
        UUID id,
        String company,
        String role,
        String status,
        String source,
        String location,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String currency,
        LocalDate nextFollowUpOn,
        String[] tags,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        LocalDate appliedAt,
        String jobLink,
        String resumeUploaded,
        boolean gotCall,
        LocalDate rejectDate,
        String loginDetails,
        List<Note> notes,
        List<StatusChange> statusHistory
) {
    public record Note(long id, String body, OffsetDateTime createdAt) {}

    public record StatusChange(long id, String fromStatus, String toStatus, OffsetDateTime changedAt) {}
}
