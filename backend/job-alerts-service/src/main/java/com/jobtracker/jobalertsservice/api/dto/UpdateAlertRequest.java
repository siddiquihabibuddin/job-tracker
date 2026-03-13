package com.jobtracker.jobalertsservice.api.dto;

import java.util.List;

public record UpdateAlertRequest(
        List<CompanyInput> companies,
        String roleKeywords,
        List<String> platforms,
        Boolean active
) {}
