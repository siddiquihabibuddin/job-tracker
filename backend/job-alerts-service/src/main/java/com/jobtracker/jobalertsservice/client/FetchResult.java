package com.jobtracker.jobalertsservice.client;

import java.util.List;

public record FetchResult(List<JobPosting> postings, String errorMessage) {

    public static FetchResult ok(List<JobPosting> postings) {
        return new FetchResult(postings, null);
    }

    public static FetchResult error(String message) {
        return new FetchResult(List.of(), message);
    }

    public boolean hasError() {
        return errorMessage != null;
    }
}
