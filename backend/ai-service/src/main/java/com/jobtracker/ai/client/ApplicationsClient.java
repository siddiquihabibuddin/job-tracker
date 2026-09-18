package com.jobtracker.ai.client;

import com.jobtracker.ai.api.dto.ApplicationContextDto;
import com.jobtracker.ai.api.dto.PageResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class ApplicationsClient {

    private static final int PAGE_SIZE = 100;

    private final RestClient restClient;

    public ApplicationsClient(RestClient applicationsRestClient) {
        this.restClient = applicationsRestClient;
    }

    public List<ApplicationContextDto> fetchAllContext(String authHeader) {
        List<ApplicationContextDto> all = new ArrayList<>();
        int page = 0;
        while (true) {
            PageResponse<ApplicationContextDto> resp = restClient.get()
                    .uri("/v1/applications/context?page={page}&limit={limit}", page, PAGE_SIZE)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<PageResponse<ApplicationContextDto>>() {});
            if (resp == null || resp.items().isEmpty()) break;
            all.addAll(resp.items());
            if (page + 1 >= resp.totalPages()) break;
            page++;
        }
        return all;
    }
}
