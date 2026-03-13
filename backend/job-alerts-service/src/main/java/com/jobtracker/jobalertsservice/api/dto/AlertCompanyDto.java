package com.jobtracker.jobalertsservice.api.dto;

import java.time.OffsetDateTime;

public record AlertCompanyDto(
        String companyName,
        String boardToken,
        String workdayTenant,
        String workdaySite,
        Integer workdayWdNum,
        String lastErrorMessage,
        OffsetDateTime lastErrorAt,
        OffsetDateTime lastSuccessAt
) {}
