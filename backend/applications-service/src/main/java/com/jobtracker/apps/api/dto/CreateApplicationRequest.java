package com.jobtracker.apps.api.dto;

import com.jobtracker.apps.domain.model.AppStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateApplicationRequest(
        @NotBlank @Size(max = 200) String company,
        @NotBlank @Size(max = 200) String role,
        AppStatus status,
        @Size(max = 100) String source,
        @Size(max = 200) String location,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        @Size(max = 3) String currency,
        LocalDate nextFollowUpOn,
        String[] tags,
        @Size(max = 5000) String notes
) {}
