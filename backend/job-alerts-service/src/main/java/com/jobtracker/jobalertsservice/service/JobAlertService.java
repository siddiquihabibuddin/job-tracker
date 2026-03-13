package com.jobtracker.jobalertsservice.service;

import com.jobtracker.jobalertsservice.api.dto.*;
import com.jobtracker.jobalertsservice.domain.AlertCompany;
import com.jobtracker.jobalertsservice.domain.JobAlert;
import com.jobtracker.jobalertsservice.domain.JobAlertMatch;
import com.jobtracker.jobalertsservice.repository.JobAlertMatchRepository;
import com.jobtracker.jobalertsservice.repository.JobAlertRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional
public class JobAlertService {

    private final JobAlertRepository alertRepo;
    private final JobAlertMatchRepository matchRepo;

    public JobAlertService(JobAlertRepository alertRepo, JobAlertMatchRepository matchRepo) {
        this.alertRepo = alertRepo;
        this.matchRepo = matchRepo;
    }

    @Transactional(readOnly = true)
    public List<JobAlertDto> listAlerts(UUID userId) {
        return alertRepo.findByUserIdAndDeletedAtIsNull(userId).stream()
                .map(this::toDto)
                .toList();
    }

    public JobAlertDto createAlert(UUID userId, CreateAlertRequest req) {
        JobAlert alert = new JobAlert();
        alert.setId(UUID.randomUUID());
        alert.setUserId(userId);
        alert.setRoleKeywords(req.roleKeywords());
        alert.setPlatforms(req.platforms() != null ? String.join(",", req.platforms()) : "");
        alert.setActive(true);

        AtomicInteger order = new AtomicInteger(0);
        for (CompanyInput ci : req.companies()) {
            AlertCompany ac = buildAlertCompany(ci, alert, order.getAndIncrement());
            alert.getCompanies().add(ac);
        }

        return toDto(alertRepo.save(alert));
    }

    public JobAlertDto updateAlert(UUID userId, UUID id, UpdateAlertRequest req) {
        JobAlert alert = loadAlertForUser(userId, id);
        if (req.roleKeywords() != null) alert.setRoleKeywords(req.roleKeywords());
        if (req.platforms() != null) alert.setPlatforms(String.join(",", req.platforms()));
        if (req.active() != null) alert.setActive(req.active());
        if (req.companies() != null) {
            alert.getCompanies().clear();
            AtomicInteger order = new AtomicInteger(0);
            for (CompanyInput ci : req.companies()) {
                AlertCompany ac = buildAlertCompany(ci, alert, order.getAndIncrement());
                alert.getCompanies().add(ac);
            }
        }
        return toDto(alertRepo.save(alert));
    }

    public void deleteAlert(UUID userId, UUID id) {
        JobAlert alert = loadAlertForUser(userId, id);
        matchRepo.deleteByAlert_Id(id);
        alert.setDeletedAt(OffsetDateTime.now());
        alertRepo.save(alert);
    }

    @Transactional(readOnly = true)
    public List<JobAlertMatchDto> listMatchesForAlert(UUID userId, UUID alertId) {
        loadAlertForUser(userId, alertId);
        return matchRepo.findByAlert_IdOrderByCreatedAtDesc(alertId).stream()
                .map(this::toMatchDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JobAlertMatchDto> listUnseenMatches(UUID userId) {
        return matchRepo.findByUserIdAndSeenAtIsNullOrderByCreatedAtDesc(userId).stream()
                .map(this::toMatchDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unseenCount(UUID userId) {
        return matchRepo.countByUserIdAndSeenAtIsNull(userId);
    }

    public void markSeen(UUID userId, UUID matchId) {
        JobAlertMatch match = matchRepo.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!match.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        match.setSeenAt(OffsetDateTime.now());
        matchRepo.save(match);
    }

    public void markAllSeen(UUID userId, UUID alertId) {
        loadAlertForUser(userId, alertId);
        List<JobAlertMatch> unseen = matchRepo.findByAlert_IdOrderByCreatedAtDesc(alertId)
                .stream()
                .filter(m -> m.getSeenAt() == null)
                .toList();
        OffsetDateTime now = OffsetDateTime.now();
        unseen.forEach(m -> m.setSeenAt(now));
        matchRepo.saveAll(unseen);
    }

    private JobAlert loadAlertForUser(UUID userId, UUID alertId) {
        JobAlert alert = alertRepo.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!alert.getUserId().equals(userId) || alert.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return alert;
    }

    private AlertCompany buildAlertCompany(CompanyInput ci, JobAlert alert, int order) {
        AlertCompany ac = new AlertCompany();
        ac.setId(UUID.randomUUID());
        ac.setAlert(alert);
        ac.setCompanyName(ci.companyName());
        ac.setBoardToken(ci.boardToken());
        ac.setWorkdayTenant(ci.workdayTenant());
        ac.setWorkdaySite(ci.workdaySite() != null ? ci.workdaySite() : "External_Career_Site");
        ac.setWorkdayWdNum(ci.workdayWdNum() != null ? ci.workdayWdNum().shortValue() : null);
        ac.setDisplayOrder(order);
        return ac;
    }

    private JobAlertDto toDto(JobAlert a) {
        List<String> platforms = a.getPlatforms() == null || a.getPlatforms().isBlank()
                ? List.of()
                : Arrays.asList(a.getPlatforms().split(","));
        List<AlertCompanyDto> companies = a.getCompanies().stream()
                .map(ac -> new AlertCompanyDto(
                        ac.getCompanyName(),
                        ac.getBoardToken(),
                        ac.getWorkdayTenant(),
                        ac.getWorkdaySite(),
                        ac.getWorkdayWdNum() != null ? ac.getWorkdayWdNum().intValue() : null,
                        ac.getLastErrorMessage(),
                        ac.getLastErrorAt(),
                        ac.getLastSuccessAt()))
                .toList();
        return new JobAlertDto(
                a.getId(), a.getUserId(), companies,
                a.getRoleKeywords(), platforms, a.isActive(),
                a.getCreatedAt(), a.getUpdatedAt()
        );
    }

    private JobAlertMatchDto toMatchDto(JobAlertMatch m) {
        return new JobAlertMatchDto(
                m.getId(), m.getAlert().getId(), m.getPlatform(), m.getTitle(),
                m.getJobUrl(), m.getCompanyName(), m.getLocation(),
                m.getPostedAt(), m.getSeenAt(), m.getCreatedAt()
        );
    }
}
