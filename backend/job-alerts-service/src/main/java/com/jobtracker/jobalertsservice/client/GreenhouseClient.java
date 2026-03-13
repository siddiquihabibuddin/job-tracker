package com.jobtracker.jobalertsservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class GreenhouseClient {

    private static final Logger log = LoggerFactory.getLogger(GreenhouseClient.class);
    private static final String BASE_URL = "https://boards-api.greenhouse.io/v1/boards";

    private final RestClient restClient;

    public GreenhouseClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public FetchResult fetchJobs(String boardToken) {
        try {
            GreenhouseResponse response = restClient.get()
                    .uri(BASE_URL + "/{token}/jobs", boardToken)
                    .retrieve()
                    .body(GreenhouseResponse.class);
            if (response == null || response.jobs() == null) return FetchResult.ok(List.of());
            return FetchResult.ok(response.jobs().stream()
                    .map(j -> new JobPosting(
                            String.valueOf(j.id()),
                            j.title(),
                            j.absoluteUrl(),
                            j.location() != null ? j.location().name() : null,
                            j.updatedAt()
                    ))
                    .toList());
        } catch (RestClientException e) {
            log.warn("Greenhouse API error for boardToken={}: {}", boardToken, e.getMessage());
            return FetchResult.error(e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GreenhouseResponse(List<GreenhouseJob> jobs) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GreenhouseJob(
            long id,
            String title,
            @JsonProperty("absolute_url") String absoluteUrl,
            GreenhouseLocation location,
            @JsonProperty("updated_at") OffsetDateTime updatedAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GreenhouseLocation(String name) {}
}
