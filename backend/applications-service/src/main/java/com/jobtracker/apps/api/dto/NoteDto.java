package com.jobtracker.apps.api.dto;

import java.time.OffsetDateTime;

public record NoteDto(
        long id,
        String body,
        OffsetDateTime createdAt
) {}