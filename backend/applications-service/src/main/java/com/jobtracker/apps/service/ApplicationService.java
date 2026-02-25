package com.jobtracker.apps.service;

import com.jobtracker.apps.api.dto.*;
import com.jobtracker.apps.api.mapper.ApplicationMapper;
import com.jobtracker.apps.domain.model.*;
import com.jobtracker.apps.domain.repo.*;
import com.jobtracker.apps.event.ApplicationEvent;
import com.jobtracker.apps.event.ApplicationEventPublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApplicationService {

    private static final UUID DEMO_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String sub = jwt.getSubject();
            if (sub != null) return UUID.fromString(sub);
        }
        return DEMO_USER_ID;
    }

    private final ApplicationRepository appRepo;
    private final UserRepository userRepo;
    private final ApplicationNoteRepository noteRepo;
    private final ApplicationStatusHistoryRepository histRepo;
    private final ApplicationEventPublisher eventPublisher;

    private final ApplicationMapper mapper = new ApplicationMapper();

    private final Counter createdCounter;
    private final Counter deletedCounter;

    public ApplicationService(ApplicationRepository appRepo,
                              UserRepository userRepo,
                              ApplicationNoteRepository noteRepo,
                              ApplicationStatusHistoryRepository histRepo,
                              ApplicationEventPublisher eventPublisher,
                              MeterRegistry meterRegistry) {
        this.appRepo = appRepo;
        this.userRepo = userRepo;
        this.noteRepo = noteRepo;
        this.histRepo = histRepo;
        this.eventPublisher = eventPublisher;
        this.createdCounter = Counter.builder("applications.created.total")
                .description("Total applications created")
                .register(meterRegistry);
        this.deletedCounter = Counter.builder("applications.deleted.total")
                .description("Total applications soft-deleted")
                .register(meterRegistry);
    }

    // --- Query ---

    public PageResponse<ApplicationDto> list(String status, String search, int page, int limit) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, limit)), Sort.by(Sort.Direction.DESC, "createdAt"));
        UUID userId = currentUserId();

        Page<Application> p;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            AppStatus st = AppStatus.valueOf(status.toUpperCase());
            p = appRepo.findAllByUser_IdAndStatusAndDeletedAtIsNull(userId, st, pageable);
        } else {
            p = appRepo.findAllByUser_IdAndDeletedAtIsNull(userId, pageable);
        }

        // simple search in-memory (company/role) for MVP
        var items = p.getContent().stream()
                .filter(a -> {
                    if (search == null || search.isBlank()) return true;
                    var q = search.toLowerCase();
                    return (a.getCompany() != null && a.getCompany().toLowerCase().contains(q))
                            || (a.getRole() != null && a.getRole().toLowerCase().contains(q));
                })
                .map(mapper::toDto)
                .toList();

        return new PageResponse<>(items, pageable.getPageSize(), p.getTotalElements(), p.getTotalPages(), p.getNumber());
    }

    public ApplicationDto get(UUID id) {
        Application a = appRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("application not found"));
        return mapper.toDto(a);
    }

    // --- Mutations ---

    @Transactional
    public ApplicationDto create(CreateApplicationRequest req) {
        var user = userRepo.findById(currentUserId()).orElseThrow();
        var a = new Application();
        a.setId(UUID.randomUUID());
        a.setUser(user);
        a.setCompany(req.company());
        a.setRole(req.role());
        a.setStatus(req.status() != null ? req.status() : AppStatus.APPLIED);
        a.setSource(req.source());
        a.setLocation(req.location());
        a.setSalaryMin(req.salaryMin());
        a.setSalaryMax(req.salaryMax());
        a.setCurrency(req.currency() != null ? req.currency() : "USD");
        a.setNextFollowUpOn(req.nextFollowUpOn());
        a.setTagsJson(mapper.tagsToJson(req.tags()));
        var saved = appRepo.saveAndFlush(a);

        if (req.notes() != null && !req.notes().isBlank()) {
            var note = new ApplicationNote();
            note.setApplication(saved);
            note.setUser(user);
            note.setBody(req.notes());
            noteRepo.save(note);
        }

        eventPublisher.publish(new ApplicationEvent(
                "APPLICATION_CREATED",
                saved.getId(),
                currentUserId(),
                saved.getStatus().name(),
                saved.getSource(),
                saved.getCreatedAt(),
                null,
                OffsetDateTime.now()));

        createdCounter.increment();
        return mapper.toDto(saved);
    }

    @Transactional
    public ApplicationDto update(UUID id, UpdateApplicationRequest req) {
        var a = appRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("application not found"));

        var prevStatus = a.getStatus();

        if (req.company() != null) a.setCompany(req.company());
        if (req.role() != null) a.setRole(req.role());
        if (req.status() != null) a.setStatus(req.status());
        if (req.source() != null) a.setSource(req.source());
        if (req.location() != null) a.setLocation(req.location());
        if (req.salaryMin() != null) a.setSalaryMin(req.salaryMin());
        if (req.salaryMax() != null) a.setSalaryMax(req.salaryMax());
        if (req.currency() != null) a.setCurrency(req.currency());
        if (req.nextFollowUpOn() != null) a.setNextFollowUpOn(req.nextFollowUpOn());
        if (req.tags() != null) a.setTagsJson(mapper.tagsToJson(req.tags()));

        appRepo.save(a);

        if (req.status() != null && req.status() != prevStatus) {
            var hist = new ApplicationStatusHistory();
            hist.setApplication(a);
            hist.setFromStatus(prevStatus);
            hist.setToStatus(req.status());
            histRepo.save(hist);
        }

        eventPublisher.publish(new ApplicationEvent(
                "APPLICATION_UPDATED",
                a.getId(),
                currentUserId(),
                a.getStatus().name(),
                a.getSource(),
                a.getCreatedAt(),
                a.getDeletedAt(),
                OffsetDateTime.now()));

        return mapper.toDto(a);
    }

    @Transactional
    public void delete(UUID id) {
        var a = appRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("application not found"));
        a.setDeletedAt(OffsetDateTime.now());
        appRepo.save(a);

        eventPublisher.publish(new ApplicationEvent(
                "APPLICATION_DELETED",
                a.getId(),
                currentUserId(),
                a.getStatus().name(),
                a.getSource(),
                a.getCreatedAt(),
                a.getDeletedAt(),
                OffsetDateTime.now()));

        deletedCounter.increment();
    }

    // Notes

    @Transactional
    public NoteDto addNote(UUID applicationId, AddNoteRequest req) {
        var a = appRepo.findById(applicationId).orElseThrow(() -> new EntityNotFoundException("application not found"));
        var u = userRepo.findById(currentUserId()).orElseThrow();
        var note = new ApplicationNote();
        note.setApplication(a);
        note.setUser(u);
        note.setBody(req.body());
        noteRepo.save(note);
        return mapper.toDto(note);
    }

    public NoteDto updateNote(UUID appId, Long noteId, UpdateNoteRequest req) {
        ApplicationNote note = noteRepo.findById(noteId).orElseThrow(() -> new EntityNotFoundException("note not found"));
        if (!note.getApplication().getId().equals(appId)) {
            throw new IllegalArgumentException("note does not belong to the specified application");
        }
        note.setBody(req.getContent());
        noteRepo.save(note);
        return mapper.toDto(note);
    }

}
