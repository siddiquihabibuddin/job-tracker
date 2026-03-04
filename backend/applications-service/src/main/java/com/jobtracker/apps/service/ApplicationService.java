package com.jobtracker.apps.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.apps.api.dto.*;
import com.jobtracker.apps.api.mapper.ApplicationMapper;
import com.jobtracker.apps.domain.model.*;
import com.jobtracker.apps.domain.repo.*;
import com.jobtracker.apps.event.ApplicationEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
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
    private final OutboxEventRepository outboxRepo;
    private final ObjectMapper objectMapper;

    private final ApplicationMapper mapper = new ApplicationMapper();

    private final Counter createdCounter;
    private final Counter deletedCounter;

    public ApplicationService(ApplicationRepository appRepo,
                              UserRepository userRepo,
                              ApplicationNoteRepository noteRepo,
                              ApplicationStatusHistoryRepository histRepo,
                              OutboxEventRepository outboxRepo,
                              ObjectMapper objectMapper,
                              MeterRegistry meterRegistry) {
        this.appRepo = appRepo;
        this.userRepo = userRepo;
        this.noteRepo = noteRepo;
        this.histRepo = histRepo;
        this.outboxRepo = outboxRepo;
        this.objectMapper = objectMapper;
        this.createdCounter = Counter.builder("applications.created.total")
                .description("Total applications created")
                .register(meterRegistry);
        this.deletedCounter = Counter.builder("applications.deleted.total")
                .description("Total applications soft-deleted")
                .register(meterRegistry);
    }

    // --- Query ---

    public PageResponse<ApplicationDto> list(String status, String search, Integer month, Integer year, Boolean gotCall, String sortBy, int page, int limit) {
        String sortField = "appliedAt".equalsIgnoreCase(sortBy) ? "appliedAt" : "createdAt";
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, limit)), Sort.by(Sort.Direction.DESC, sortField));
        UUID userId = currentUserId();

        Specification<Application> spec = buildSpec(userId, status, search, month, year, gotCall);
        Page<Application> p = appRepo.findAll(spec, pageable);

        var items = p.getContent().stream().map(mapper::toDto).toList();

        return new PageResponse<>(items, pageable.getPageSize(), p.getTotalElements(), p.getTotalPages(), p.getNumber());
    }

    private Specification<Application> buildSpec(UUID userId, String status, String search, Integer month, Integer year, Boolean gotCall) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("company")), pattern),
                        cb.like(cb.lower(root.get("role")), pattern)
                ));
            }
            if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
                predicates.add(cb.equal(root.get("status"), AppStatus.valueOf(status.toUpperCase())));
            }
            if (month != null) {
                var monthExpr = cb.function("date_part", Double.class,
                        cb.literal("month"), root.get("appliedAt"));
                predicates.add(cb.equal(monthExpr, month.doubleValue()));
            }
            if (year != null) {
                var yearExpr = cb.function("date_part", Double.class,
                        cb.literal("year"), root.get("appliedAt"));
                predicates.add(cb.equal(yearExpr, year.doubleValue()));
            }
            if (gotCall != null) {
                predicates.add(cb.equal(root.get("gotCall"), gotCall));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
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
        a.setAppliedAt(req.appliedAt());
        a.setJobLink(req.jobLink());
        a.setResumeUploaded(req.resumeUploaded());
        if (req.gotCall() != null) a.setGotCall(req.gotCall());
        a.setRejectDate(req.rejectDate());
        a.setLoginDetails(req.loginDetails());
        var saved = appRepo.saveAndFlush(a);

        if (req.notes() != null && !req.notes().isBlank()) {
            var note = new ApplicationNote();
            note.setApplication(saved);
            note.setUser(user);
            note.setBody(req.notes());
            noteRepo.save(note);
        }

        outboxRepo.save(toOutboxEvent(new ApplicationEvent(
                "APPLICATION_CREATED",
                saved.getId(),
                currentUserId(),
                saved.getStatus().name(),
                saved.getSource(),
                saved.getCreatedAt(),
                null,
                OffsetDateTime.now(),
                saved.getAppliedAt())));

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
        if (req.appliedAt() != null) a.setAppliedAt(req.appliedAt());
        if (req.jobLink() != null) a.setJobLink(req.jobLink());
        if (req.resumeUploaded() != null) a.setResumeUploaded(req.resumeUploaded());
        if (req.gotCall() != null) a.setGotCall(req.gotCall());
        if (req.rejectDate() != null) a.setRejectDate(req.rejectDate());
        if (req.loginDetails() != null) a.setLoginDetails(req.loginDetails());

        appRepo.save(a);

        if (req.status() != null && req.status() != prevStatus) {
            var hist = new ApplicationStatusHistory();
            hist.setApplication(a);
            hist.setFromStatus(prevStatus);
            hist.setToStatus(req.status());
            histRepo.save(hist);
        }

        outboxRepo.save(toOutboxEvent(new ApplicationEvent(
                "APPLICATION_UPDATED",
                a.getId(),
                currentUserId(),
                a.getStatus().name(),
                a.getSource(),
                a.getCreatedAt(),
                a.getDeletedAt(),
                OffsetDateTime.now(),
                a.getAppliedAt())));

        return mapper.toDto(a);
    }

    @Transactional
    public void delete(UUID id) {
        var a = appRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("application not found"));
        a.setDeletedAt(OffsetDateTime.now());
        appRepo.save(a);

        outboxRepo.save(toOutboxEvent(new ApplicationEvent(
                "APPLICATION_DELETED",
                a.getId(),
                currentUserId(),
                a.getStatus().name(),
                a.getSource(),
                a.getCreatedAt(),
                a.getDeletedAt(),
                OffsetDateTime.now(),
                a.getAppliedAt())));

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

    private OutboxEvent toOutboxEvent(ApplicationEvent event) {
        try {
            var o = new OutboxEvent();
            o.setId(UUID.randomUUID());
            o.setAggregateId(event.id());
            o.setEventType(event.eventType());
            o.setPayload(objectMapper.writeValueAsString(event));
            o.setCreatedAt(OffsetDateTime.now());
            return o;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize ApplicationEvent to outbox payload", e);
        }
    }

}
