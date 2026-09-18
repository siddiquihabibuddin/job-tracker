package com.jobtracker.apps.api.dto;

import com.jobtracker.apps.domain.model.AppStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ApplicationContextDto(
        UUID id,
        String company,
        String role,
        AppStatus status,
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
        List<NoteDto> notes,
        List<StatusChangeDto> statusHistory
) {}
