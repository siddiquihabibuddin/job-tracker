package com.jobtracker.stats.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobtracker.stats.api.dto.InsightsDto;
import com.jobtracker.stats.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InsightsService {

    private static final Logger log = LoggerFactory.getLogger(InsightsService.class);

    private final StatsService statsService;
    private final CacheManager cacheManager;
    private final RestClient restClient;
    private final String model;
    private final ObjectMapper objectMapper;

    public InsightsService(
            StatsService statsService,
            CacheManager cacheManager,
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.model}") String model) {
        this.statsService = statsService;
        this.cacheManager = cacheManager;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public InsightsDto generateInsights(UUID userId) {
        // Check cache manually so we can skip caching fallback responses
        Cache cache = cacheManager.getCache(CacheConfig.CACHE_INSIGHTS);
        if (cache != null) {
            Cache.ValueWrapper cached = cache.get(userId.toString());
            if (cached != null) return (InsightsDto) cached.get();
        }
        try {
            int currentYear = LocalDate.now().getYear();

            Map<String, Object> statsData = new LinkedHashMap<>();
            statsData.put("summary_30d", statsService.getSummary(userId, 30));
            statsData.put("trend_12w", statsService.getTrend(userId, 12));
            statsData.put("breakdown_current_year", statsService.getBreakdown(userId, "month", currentYear, null));
            statsData.put("role_distribution_current_year", statsService.getRoleCounts(userId, "month", currentYear));

            String statsJson = objectMapper.writeValueAsString(statsData);

            String prompt = """
                    You are a job search coach. Analyze this user's job application data.
                    Respond in English only.
                    Return ONLY a valid JSON array of 3-5 concise, actionable insight strings. No explanation, no markdown, just the array.
                    Focus on: response rates by source, ghosting patterns, application velocity, role trends, anomalies.

                    Data: """ + statsJson;

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "stream", false
            );

            String requestJson = objectMapper.writeValueAsString(requestBody);

            String responseBody = restClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestJson)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.at("/choices/0/message/content").asText();

            // Extract the JSON array — find first '[' to matching ']', ignoring any surrounding text or code fences
            int start = content.indexOf('[');
            int end = content.lastIndexOf(']');
            if (start == -1 || end == -1 || end <= start) {
                throw new IllegalArgumentException("No JSON array found in LLM response: " + content);
            }
            content = content.substring(start, end + 1);

            List<String> insights = objectMapper.readValue(
                    content,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

            InsightsDto result = new InsightsDto(insights, OffsetDateTime.now().toString());
            log.info("Generated {} insights for userId={}", insights.size(), userId);
            if (cache != null) cache.put(userId.toString(), result);
            return result;

        } catch (Exception e) {
            log.warn("Failed to generate insights for userId={}: {}", userId, e.getMessage());
            return new InsightsDto(
                    List.of("Insights unavailable — LLM service is starting up."),
                    OffsetDateTime.now().toString());
        }
    }
}
