package com.jobtracker.apps.api.dto;

import com.jobtracker.apps.domain.model.AppStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateApplicationRequest(
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
        LocalDate appliedAt,
        String jobLink,
        String resumeUploaded,
        Boolean gotCall,
        LocalDate rejectDate,
        String loginDetails
) {}