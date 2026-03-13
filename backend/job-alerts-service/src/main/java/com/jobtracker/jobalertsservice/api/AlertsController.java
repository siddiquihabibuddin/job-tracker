package com.jobtracker.jobalertsservice.api;

import com.jobtracker.jobalertsservice.api.dto.*;
import com.jobtracker.jobalertsservice.service.JobAlertPoller;
import com.jobtracker.jobalertsservice.service.JobAlertService;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/alerts")
public class AlertsController {

    private static final UUID DEMO_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final JobAlertService service;
    private final JobAlertPoller poller;

    public AlertsController(JobAlertService service, @Nullable JobAlertPoller poller) {
        this.service = service;
        this.poller = poller;
    }

    @GetMapping
    public List<JobAlertDto> listAlerts(@AuthenticationPrincipal Jwt jwt) {
        return service.listAlerts(currentUserId(jwt));
    }

    @PostMapping
    public ResponseEntity<JobAlertDto> createAlert(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateAlertRequest req,
            UriComponentsBuilder uriBuilder) {
        JobAlertDto dto = service.createAlert(currentUserId(jwt), req);
        return ResponseEntity
                .created(uriBuilder.path("/v1/alerts/{id}").buildAndExpand(dto.id()).toUri())
                .body(dto);
    }

    @PatchMapping("/{id}")
    public JobAlertDto updateAlert(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody UpdateAlertRequest req) {
        return service.updateAlert(currentUserId(jwt), id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.deleteAlert(currentUserId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{alertId}/matches")
    public List<JobAlertMatchDto> listMatchesForAlert(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID alertId) {
        return service.listMatchesForAlert(currentUserId(jwt), alertId);
    }

    @GetMapping("/matches/unseen")
    public List<JobAlertMatchDto> listUnseenMatches(@AuthenticationPrincipal Jwt jwt) {
        return service.listUnseenMatches(currentUserId(jwt));
    }

    @GetMapping("/matches/unseen/count")
    public Map<String, Long> unseenCount(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("count", service.unseenCount(currentUserId(jwt)));
    }

    @PostMapping("/matches/{matchId}/seen")
    public ResponseEntity<Void> markSeen(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID matchId) {
        service.markSeen(currentUserId(jwt), matchId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{alertId}/matches/seen-all")
    public ResponseEntity<Void> markAllSeen(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID alertId) {
        service.markAllSeen(currentUserId(jwt), alertId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/poll")
    public ResponseEntity<Void> pollNow() {
        if (poller == null) return ResponseEntity.noContent().build();
        poller.poll();
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId(Jwt jwt) {
        if (jwt == null) return DEMO_USER_ID;
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (Exception e) {
            return DEMO_USER_ID;
        }
    }
}
