package com.jobtracker.jobalertsservice.client;

import java.time.OffsetDateTime;

public record JobPosting(
        String externalId,
        String title,
        String jobUrl,
        String location,
        OffsetDateTime postedAt
) {}
