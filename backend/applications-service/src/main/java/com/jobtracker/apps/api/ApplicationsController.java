package com.jobtracker.apps.api;

import com.jobtracker.apps.api.dto.*;
import com.jobtracker.apps.service.ApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/v1/applications")
@Validated
public class ApplicationsController {

    private final ApplicationService svc;

    public ApplicationsController(ApplicationService svc) {
        this.svc = svc;
    }

    // List with basic filters
    @GetMapping
    public PageResponse<ApplicationDto> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) @Max(10000) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit
    ) {
        return svc.list(status, search, page, limit);
    }

    // Get by id
    @GetMapping("/{id}")
    public ApplicationDto get(@PathVariable UUID id) {
        return svc.get(id);
    }

    // Create
    @PostMapping
    public ResponseEntity<ApplicationDto> create(@Valid @RequestBody CreateApplicationRequest req) {
        var dto = svc.create(req);
        return ResponseEntity
                .created(URI.create("/v1/applications/" + dto.id()))
                .body(dto);
    }

    // Patch
    @PatchMapping("/{id}")
    public ApplicationDto update(@PathVariable UUID id, @Valid @RequestBody UpdateApplicationRequest req) {
        return svc.update(id, req);
    }

    // Soft delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        svc.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Notes
    @PostMapping("/{id}/notes")
    public NoteDto addNote(@PathVariable UUID id, @Valid @RequestBody AddNoteRequest req) {
        return svc.addNote(id, req);
    }

    // Update note by noteId
    @PatchMapping("/{appId}/notes/{noteId}")
    public NoteDto updateNote(@PathVariable UUID appId, @PathVariable Long noteId, @Valid @RequestBody UpdateNoteRequest req) {
        return svc.updateNote(appId, noteId, req);
    }

}
