package com.jobtracker.jobalertsservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Component
public class WorkdayClient {

    private static final Logger log = LoggerFactory.getLogger(WorkdayClient.class);
    private static final DateTimeFormatter WD_DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private final RestClient restClient;

    public WorkdayClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public FetchResult fetchJobs(String tenant, short wdNum, String site) {
        String host = "https://" + tenant + ".wd" + wdNum + ".myworkdayjobs.com";
        String url = host + "/wday/cxs/" + tenant + "/" + site + "/jobs";
        try {
            WorkdayResponse response = restClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .body(Map.of("limit", 20, "offset", 0, "searchText", ""))
                    .retrieve()
                    .body(WorkdayResponse.class);
            if (response == null || response.jobPostings() == null) return FetchResult.ok(List.of());
            return FetchResult.ok(response.jobPostings().stream()
                    .map(p -> {
                        OffsetDateTime postedAt = null;
                        if (p.postedOn() != null) {
                            try {
                                postedAt = LocalDate.parse(p.postedOn(), WD_DATE)
                                        .atStartOfDay(ZoneOffset.UTC)
                                        .toOffsetDateTime();
                            } catch (DateTimeParseException ignored) {}
                        }
                        return new JobPosting(
                                p.externalPath(),
                                p.title(),
                                host + p.externalPath(),
                                p.locationsText(),
                                postedAt
                        );
                    })
                    .toList());
        } catch (RestClientException e) {
            log.warn("Workday API error for tenant={}: {}", tenant, e.getMessage());
            return FetchResult.error(e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WorkdayResponse(List<WorkdayPosting> jobPostings) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record WorkdayPosting(String title, String externalPath, String postedOn, String locationsText) {}
}
