package com.jobtracker.apps.api.dto;

import com.jobtracker.apps.domain.model.AppStatus;
import java.time.OffsetDateTime;

public record StatusChangeDto(
        long id,
        AppStatus fromStatus,
        AppStatus toStatus,
        OffsetDateTime changedAt
) {}