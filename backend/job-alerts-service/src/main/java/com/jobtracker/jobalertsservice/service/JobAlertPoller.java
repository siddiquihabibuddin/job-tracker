package com.jobtracker.jobalertsservice.service;

import com.jobtracker.jobalertsservice.client.FetchResult;
import com.jobtracker.jobalertsservice.client.GreenhouseClient;
import com.jobtracker.jobalertsservice.client.JobPosting;
import com.jobtracker.jobalertsservice.client.LeverClient;
import com.jobtracker.jobalertsservice.client.WorkdayClient;
import com.jobtracker.jobalertsservice.domain.AlertCompany;
import com.jobtracker.jobalertsservice.domain.JobAlert;
import com.jobtracker.jobalertsservice.domain.JobAlertMatch;
import com.jobtracker.jobalertsservice.repository.AlertCompanyRepository;
import com.jobtracker.jobalertsservice.repository.JobAlertMatchRepository;
import com.jobtracker.jobalertsservice.repository.JobAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "job-alerts.poller.enabled", havingValue = "true", matchIfMissing = true)
public class JobAlertPoller {

    private static final Logger log = LoggerFactory.getLogger(JobAlertPoller.class);

    private final JobAlertRepository alertRepo;
    private final JobAlertMatchRepository matchRepo;
    private final AlertCompanyRepository alertCompanyRepo;
    private final GreenhouseClient greenhouseClient;
    private final LeverClient leverClient;
    private final WorkdayClient workdayClient;

    public JobAlertPoller(JobAlertRepository alertRepo, JobAlertMatchRepository matchRepo,
                          AlertCompanyRepository alertCompanyRepo,
                          GreenhouseClient greenhouseClient, LeverClient leverClient,
                          WorkdayClient workdayClient) {
        this.alertRepo = alertRepo;
        this.matchRepo = matchRepo;
        this.alertCompanyRepo = alertCompanyRepo;
        this.greenhouseClient = greenhouseClient;
        this.leverClient = leverClient;
        this.workdayClient = workdayClient;
    }

    @Scheduled(fixedDelayString = "${job-alerts.poller.interval-ms:1800000}")
    @Transactional
    public void poll() {
        List<JobAlert> activeAlerts = alertRepo.findByActiveIsTrueAndDeletedAtIsNull();
        log.info("Polling {} active alerts", activeAlerts.size());
        for (JobAlert alert : activeAlerts) {
            String[] keywords = alert.getRoleKeywords().toLowerCase().split(",");
            for (AlertCompany company : alert.getCompanies()) {
                Map<String, String> platformErrors = new LinkedHashMap<>();

                for (String platformStr : alert.getPlatforms().split(",")) {
                    String p = platformStr.trim().toUpperCase();
                    FetchResult result = switch (p) {
                        case "GREENHOUSE" -> company.getBoardToken() != null
                                ? greenhouseClient.fetchJobs(company.getBoardToken()) : FetchResult.ok(List.of());
                        case "LEVER" -> company.getBoardToken() != null
                                ? leverClient.fetchJobs(company.getBoardToken()) : FetchResult.ok(List.of());
                        case "WORKDAY" -> (company.getWorkdayTenant() != null && company.getWorkdayWdNum() != null)
                                ? workdayClient.fetchJobs(
                                    company.getWorkdayTenant(),
                                    company.getWorkdayWdNum(),
                                    company.getWorkdaySite() != null ? company.getWorkdaySite() : "External_Career_Site")
                                : FetchResult.ok(List.of());
                        default -> FetchResult.ok(List.of());
                    };

                    if (result.hasError()) {
                        platformErrors.put(p, result.errorMessage());
                    }

                    for (JobPosting posting : result.postings()) {
                        boolean matches = Arrays.stream(keywords)
                                .map(String::trim)
                                .anyMatch(kw -> posting.title().toLowerCase().contains(kw));
                        if (!matches) continue;
                        if (matchRepo.existsByAlert_IdAndPlatformAndExternalId(alert.getId(), p, posting.externalId())) continue;
                        JobAlertMatch match = new JobAlertMatch();
                        match.setId(UUID.randomUUID());
                        match.setAlert(alert);
                        match.setUserId(alert.getUserId());
                        match.setPlatform(p);
                        match.setExternalId(posting.externalId());
                        match.setTitle(posting.title());
                        match.setJobUrl(posting.jobUrl());
                        match.setCompanyName(company.getCompanyName());
                        match.setLocation(posting.location());
                        match.setPostedAt(posting.postedAt());
                        matchRepo.save(match);
                        log.debug("Saved new match: alertId={} company={} platform={} title={}",
                                alert.getId(), company.getCompanyName(), p, posting.title());
                    }
                }

                if (!platformErrors.isEmpty()) {
                    String combined = platformErrors.entrySet().stream()
                            .map(e -> e.getKey() + ": " + e.getValue())
                            .collect(Collectors.joining("\n"));
                    company.setLastErrorMessage(combined);
                    company.setLastErrorAt(OffsetDateTime.now());
                } else {
                    company.setLastErrorMessage(null);
                    company.setLastErrorAt(null);
                    company.setLastSuccessAt(OffsetDateTime.now());
                }
                alertCompanyRepo.save(company);
            }
        }
    }
}
