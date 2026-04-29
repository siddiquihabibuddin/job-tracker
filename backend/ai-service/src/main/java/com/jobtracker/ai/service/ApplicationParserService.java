package com.jobtracker.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobtracker.ai.api.dto.ParsedApplicationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ApplicationParserService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationParserService.class);

    private static final String PROMPT_TEMPLATE = """
            You are extracting structured job-application data from a plain-text description.
            Today's date is {today}.

            Return ONLY a JSON object with no markdown fences and no commentary.
            Omit any field you cannot confidently extract — do not guess.

            Fields and rules:
            - company      (string) — the employer name
            - role         (string) — the job title
            - status       (string) — one of exactly: APPLIED, PHONE, ONSITE, OFFER, REJECTED, ACCEPTED, WITHDRAWN.
                           Default to APPLIED if the description implies a fresh application.
            - source       (string) — e.g. "LinkedIn", "Referral", "Indeed", "Company website"
            - location     (string) — city, region, or "Remote"
            - salaryMin    (number) — strip $ and commas, expand k/K to thousands (100k → 100000).
                           If a single salary number is given, set both salaryMin and salaryMax to it.
                           If only "up to X" is given, set only salaryMax. If only "starting at X", set only salaryMin.
            - salaryMax    (number) — see salaryMin rules
            - currency     (string) — 3-letter ISO code; default "USD" when a $ salary is present
            - appliedAt    (string, ISO date YYYY-MM-DD) — resolve relative dates ("today", "yesterday", "last Monday") against today's date
            - nextFollowUpOn (string, ISO date YYYY-MM-DD) — only if explicitly mentioned
            - jobLink      (string) — URL if present
            - tags         (array of strings) — explicit skills/keywords/tags mentioned by the user
            - notes        (string) — free-form context that does not fit any other field

            Description:
            {description}
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public ApplicationParserService(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public ParsedApplicationDto parse(String description) {
        String today = LocalDate.now().toString();

        String content = chatClient.prompt()
                .user(u -> u.text(PROMPT_TEMPLATE)
                        .param("today", today)
                        .param("description", description))
                .call()
                .content();

        try {
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start == -1 || end == -1 || end <= start) {
                throw new IllegalArgumentException("No JSON object found in LLM response");
            }
            String json = content.substring(start, end + 1);
            ParsedApplicationDto result = objectMapper.readValue(json, ParsedApplicationDto.class);
            log.info("Parsed application: company={}, role={}", result.company(), result.role());
            return result;
        } catch (Exception e) {
            String truncated = content.length() > 500 ? content.substring(0, 500) : content;
            log.warn("Failed to parse LLM response: {}", truncated);
            throw e instanceof RuntimeException re ? re : new RuntimeException(e);
        }
    }
}
