package com.jobtracker.jobalertsservice.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateAlertRequest(
        @NotEmpty List<CompanyInput> companies,
        @NotBlank String roleKeywords,
        List<String> platforms
) {}
