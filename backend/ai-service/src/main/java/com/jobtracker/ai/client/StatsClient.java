package com.jobtracker.ai.client;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class StatsClient {

    private final RestClient restClient;

    public StatsClient(RestClient statsRestClient) {
        this.restClient = statsRestClient;
    }

    public Map<String, Object> fetchAggregate(String authHeader, int windowDays) {
        int currentYear = LocalDate.now().getYear();
        Map<String, Object> aggregate = new LinkedHashMap<>();
        aggregate.put("summary_window", get(authHeader, "/v1/stats/summary?window=" + windowDays + "d"));
        aggregate.put("trend_12w", get(authHeader, "/v1/stats/trend?period=week&weeks=12"));
        aggregate.put("breakdown_current_year", get(authHeader, "/v1/stats/breakdown?groupBy=month&year=" + currentYear));
        aggregate.put("role_distribution_current_year", get(authHeader, "/v1/stats/roles?groupBy=month&year=" + currentYear));
        return aggregate;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> get(String authHeader, String path) {
        return restClient.get()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .retrieve()
                .body(Map.class);
    }
}
