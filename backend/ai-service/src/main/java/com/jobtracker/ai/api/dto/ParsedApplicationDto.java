package com.jobtracker.ai.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ParsedApplicationDto(
        String company,
        String role,
        String status,
        String source,
        String location,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String currency,
        LocalDate appliedAt,
        LocalDate nextFollowUpOn,
        String jobLink,
        List<String> tags,
        String notes
) {}
