package com.jobtracker.ai.index;

import com.jobtracker.ai.api.dto.ApplicationContextDto;
import com.jobtracker.ai.client.ApplicationsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps the pgvector index in sync with a user's live application data.
 * Only re-embeds applications whose content actually changed since the last sync
 * (compared via a content hash stored in each document's metadata).
 */
@Service
public class ApplicationIndexService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationIndexService.class);

    private final VectorStore vectorStore;
    private final ApplicationsClient applicationsClient;
    private final JdbcTemplate jdbcTemplate;

    public ApplicationIndexService(VectorStore vectorStore, ApplicationsClient applicationsClient, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.applicationsClient = applicationsClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void sync(String authHeader, UUID userId) {
        List<ApplicationContextDto> current = applicationsClient.fetchAllContext(authHeader);
        Map<String, String> storedHashes = fetchStoredHashes(userId);

        Set<String> currentIds = new HashSet<>();
        int reembedded = 0;

        for (ApplicationContextDto app : current) {
            String id = app.id().toString();
            currentIds.add(id);
            String content = renderContent(app);
            String hash = sha256(content);

            if (!hash.equals(storedHashes.get(id))) {
                Document doc = Document.builder()
                        .id(id)
                        .text(content)
                        .metadata(Map.of(
                                "userId", userId.toString(),
                                "applicationId", id,
                                "status", app.status() == null ? "" : app.status(),
                                "company", app.company() == null ? "" : app.company(),
                                "contentHash", hash
                        ))
                        .build();
                vectorStore.add(List.of(doc));
                reembedded++;
            }
        }

        List<String> stale = storedHashes.keySet().stream()
                .filter(id -> !currentIds.contains(id))
                .toList();
        if (!stale.isEmpty()) {
            vectorStore.delete(stale);
        }

        log.info("Synced RAG index for userId={}: {} total, {} re-embedded, {} removed",
                userId, current.size(), reembedded, stale.size());
    }

    private Map<String, String> fetchStoredHashes(UUID userId) {
        Map<String, String> result = new HashMap<>();
        jdbcTemplate.query(
                "SELECT id, metadata->>'contentHash' AS hash FROM vector_store WHERE metadata->>'userId' = ?",
                rs -> {
                    result.put(rs.getString("id"), rs.getString("hash"));
                },
                userId.toString());
        return result;
    }

    private String renderContent(ApplicationContextDto a) {
        StringBuilder sb = new StringBuilder();
        sb.append("Company: ").append(a.company()).append('\n');
        sb.append("Role: ").append(a.role()).append('\n');
        sb.append("Status: ").append(a.status()).append('\n');
        if (a.source() != null) sb.append("Source: ").append(a.source()).append('\n');
        if (a.location() != null) sb.append("Location: ").append(a.location()).append('\n');
        if (a.salaryMin() != null || a.salaryMax() != null) {
            sb.append("Salary: ").append(a.salaryMin()).append('-').append(a.salaryMax())
                    .append(' ').append(a.currency() == null ? "" : a.currency()).append('\n');
        }
        if (a.appliedAt() != null) sb.append("Applied at: ").append(a.appliedAt()).append('\n');
        if (a.nextFollowUpOn() != null) sb.append("Next follow-up: ").append(a.nextFollowUpOn()).append('\n');
        if (a.rejectDate() != null) sb.append("Rejected on: ").append(a.rejectDate()).append('\n');
        if (a.tags() != null && a.tags().length > 0) sb.append("Tags: ").append(String.join(", ", a.tags())).append('\n');
        sb.append("Got call: ").append(a.gotCall()).append('\n');
        sb.append("Created at: ").append(a.createdAt()).append('\n');

        if (a.notes() != null && !a.notes().isEmpty()) {
            sb.append("Notes:\n");
            for (var n : a.notes()) {
                sb.append("  - [").append(n.createdAt()).append("] ").append(n.body()).append('\n');
            }
        }
        if (a.statusHistory() != null && !a.statusHistory().isEmpty()) {
            sb.append("Status history:\n");
            for (var h : a.statusHistory()) {
                sb.append("  - ").append(h.fromStatus()).append(" -> ").append(h.toStatus())
                        .append(" @ ").append(h.changedAt()).append('\n');
            }
        }
        return sb.toString();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
