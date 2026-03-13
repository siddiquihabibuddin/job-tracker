package com.jobtracker.jobalertsservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class LeverClient {

    private static final Logger log = LoggerFactory.getLogger(LeverClient.class);
    private static final String BASE_URL = "https://api.lever.co/v0/postings";

    private final RestClient restClient;

    public LeverClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public FetchResult fetchJobs(String slug) {
        try {
            LeverPosting[] postings = restClient.get()
                    .uri(BASE_URL + "/{slug}?mode=json", slug)
                    .retrieve()
                    .body(LeverPosting[].class);
            if (postings == null) return FetchResult.ok(List.of());
            return FetchResult.ok(List.of(postings).stream()
                    .map(p -> new JobPosting(
                            p.id(),
                            p.text(),
                            p.hostedUrl(),
                            p.categories() != null ? p.categories().location() : null,
                            p.createdAt() != null
                                    ? OffsetDateTime.ofInstant(Instant.ofEpochMilli(p.createdAt()), ZoneOffset.UTC)
                                    : null
                    ))
                    .toList());
        } catch (RestClientException e) {
            log.warn("Lever API error for slug={}: {}", slug, e.getMessage());
            return FetchResult.error(e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LeverPosting(
            String id,
            String text,
            String hostedUrl,
            Long createdAt,
            LeverCategories categories
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LeverCategories(String location) {}
}
