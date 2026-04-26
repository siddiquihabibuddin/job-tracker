package com.jobtracker.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobtracker.ai.api.dto.InsightsDto;
import com.jobtracker.ai.client.StatsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiInsightsService {

    private static final Logger log = LoggerFactory.getLogger(AiInsightsService.class);

    private static final String PROMPT_TEMPLATE = """
            Analyze this user's job application data.
            Return ONLY a valid JSON array of 3-5 concise, actionable insight strings. No explanation, no markdown, just the array.
            Focus on: response rates by source, ghosting patterns, application velocity, role trends, anomalies.

            Data: {data}
            """;

    private final ChatClient chatClient;
    private final StatsClient statsClient;
    private final ObjectMapper objectMapper;

    public AiInsightsService(ChatClient chatClient, StatsClient statsClient) {
        this.chatClient = chatClient;
        this.statsClient = statsClient;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public InsightsDto generateInsights(UUID userId, String authHeader, int windowDays) {
        try {
            Map<String, Object> aggregate = statsClient.fetchAggregate(authHeader, windowDays);
            String statsJson = objectMapper.writeValueAsString(aggregate);

            String content = chatClient.prompt()
                    .user(u -> u.text(PROMPT_TEMPLATE).param("data", statsJson))
                    .call()
                    .content();

            List<String> insights = parseInsightsArray(content);
            log.info("Generated {} insights via Spring AI for userId={}", insights.size(), userId);
            return new InsightsDto(insights, OffsetDateTime.now().toString());

        } catch (Exception e) {
            log.warn("Failed to generate insights for userId={}: {}", userId, e.getMessage());
            return new InsightsDto(
                    List.of("Insights unavailable — LLM service is starting up."),
                    OffsetDateTime.now().toString());
        }
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
