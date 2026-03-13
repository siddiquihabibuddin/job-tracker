package com.jobtracker.jobalertsservice.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CompanyInput(
        @NotBlank String companyName,
        String boardToken,
        String workdayTenant,
        String workdaySite,
        Integer workdayWdNum
) {}
