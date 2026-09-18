package com.jobtracker.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobtracker.ai.api.dto.InsightsDto;
import com.jobtracker.ai.client.StatsClient;
import com.jobtracker.ai.index.ApplicationIndexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiInsightsService {

    private static final Logger log = LoggerFactory.getLogger(AiInsightsService.class);

    /** Fixed retrieval queries covering the insight categories we want the LLM to reason about. */
    private static final List<String> RETRIEVAL_QUERIES = List.of(
            "applications with no response for a long time, ghosting, stalled in the same status",
            "applications that progressed quickly to phone screen, onsite, or offer",
            "notable notes, red flags, or unusual circumstances mentioned about the application"
    );
    private static final int TOP_K_PER_QUERY = 8;
    private static final int MAX_RETRIEVED_DOCS = 20;

    private static final String PROMPT_TEMPLATE = """
            Analyze this user's job application data.
            Return ONLY a valid JSON array of 3-5 concise, actionable insight strings. No explanation, no markdown, just the array.
            Focus on: response rates by source, ghosting patterns, application velocity, role trends, anomalies.
            Where useful, cite specific companies, roles, or timeframes from the application records below rather than
            speaking only in generalities.

            Aggregate stats: {stats}

            Relevant application records:
            {records}
            """;

    private final ChatClient chatClient;
    private final StatsClient statsClient;
    private final VectorStore vectorStore;
    private final ApplicationIndexService applicationIndexService;
    private final ObjectMapper objectMapper;

    public AiInsightsService(ChatClient chatClient, StatsClient statsClient, VectorStore vectorStore,
                              ApplicationIndexService applicationIndexService) {
        this.chatClient = chatClient;
        this.statsClient = statsClient;
        this.vectorStore = vectorStore;
        this.applicationIndexService = applicationIndexService;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public InsightsDto generateInsights(UUID userId, String authHeader, int windowDays) {
        try {
            applicationIndexService.sync(authHeader, userId);

            Map<String, Object> aggregate = statsClient.fetchAggregate(authHeader, windowDays);
            String statsJson = objectMapper.writeValueAsString(aggregate);

            String recordsText = retrieveRecordsText(userId);

            String content = chatClient.prompt()
                    .user(u -> u.text(PROMPT_TEMPLATE).param("stats", statsJson).param("records", recordsText))
                    .call()
                    .content();

            List<String> insights = parseInsightsArray(content);
            log.info("Generated {} RAG-grounded insights for userId={}", insights.size(), userId);
            return new InsightsDto(insights, OffsetDateTime.now().toString());

        } catch (Exception e) {
            log.warn("Failed to generate insights for userId={}: {}", userId, e.getMessage());
            return new InsightsDto(
                    List.of("Insights unavailable — LLM service is starting up."),
                    OffsetDateTime.now().toString());
        }
    }

    private String retrieveRecordsText(UUID userId) {
        Map<String, Document> byId = new LinkedHashMap<>();
        String filter = "userId == '" + userId + "'";

        for (String query : RETRIEVAL_QUERIES) {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(TOP_K_PER_QUERY)
                    .filterExpression(filter)
                    .build();
            for (Document doc : vectorStore.similaritySearch(request)) {
                byId.putIfAbsent(doc.getId(), doc);
                if (byId.size() >= MAX_RETRIEVED_DOCS) break;
            }
            if (byId.size() >= MAX_RETRIEVED_DOCS) break;
        }

        if (byId.isEmpty()) {
            return "(no application records indexed yet)";
        }

        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Document doc : byId.values()) {
            sb.append("--- Application ").append(i++).append(" ---\n");
            sb.append(doc.getText()).append('\n');
        }
        return sb.toString();
    }

    private List<String> parseInsightsArray(String content) throws Exception {
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start == -1 || end == -1 || end <= start) {
            throw new IllegalArgumentException("No JSON array found in LLM response: " + content);
        }
        String json = content.substring(start, end + 1);
        return objectMapper.readValue(
                json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }
}
