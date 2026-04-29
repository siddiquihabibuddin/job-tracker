package com.jobtracker.apps.api;

import com.jobtracker.apps.api.dto.*;
import com.jobtracker.apps.security.PremiumGuard;
import com.jobtracker.apps.service.ApplicationService;
import com.jobtracker.apps.service.CsvImportService;
import com.jobtracker.apps.service.FolderImportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/applications")
@Validated
public class ApplicationsController {

    private final ApplicationService svc;
    private final CsvImportService csvImportService;
    private final FolderImportService folderImportService;

    public ApplicationsController(ApplicationService svc, CsvImportService csvImportService, FolderImportService folderImportService) {
        this.svc = svc;
        this.csvImportService = csvImportService;
        this.folderImportService = folderImportService;
    }

    public record BulkDeleteRequest(
            @NotEmpty @Size(max = 200) List<UUID> ids) {}

    public record BulkDeleteResponse(int deleted, List<UUID> skipped) {}

    // List with basic filters
    @GetMapping
    public PageResponse<ApplicationDto> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Boolean gotCall,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "0") @Min(0) @Max(10000) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit
    ) {
        return svc.list(status, search, month, year, gotCall, sortBy, page, limit);
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

    // Resync stats snapshot from live application data
    @PostMapping("/resync-stats")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resyncStats() {
        svc.resyncStats();
    }

    // CSV import — premium only
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CsvImportResult importCsv(Authentication auth, @RequestParam("file") MultipartFile file) throws IOException {
        PremiumGuard.requirePremium(auth);
        return csvImportService.importCsv(file);
    }

    // Folder-based bulk import — premium only
    @PostMapping("/import-folder")
    public FolderImportResult importFolder(Authentication auth) throws IOException {
        PremiumGuard.requirePremium(auth);
        return folderImportService.importFromFolder();
    }

    // Bulk soft-delete — premium only
    @PostMapping("/bulk-delete")
    public BulkDeleteResponse bulkDelete(Authentication auth, @Valid @RequestBody BulkDeleteRequest req) {
        PremiumGuard.requirePremium(auth);
        return svc.bulkDelete(req.ids());
    }
}
