package com.jobtracker.ai.api;

import com.jobtracker.ai.api.dto.InsightsDto;
import com.jobtracker.ai.api.dto.InsightsRequest;
import com.jobtracker.ai.service.AiInsightsService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/ai")
public class AiInsightsController {

    private final AiInsightsService aiInsightsService;

    public AiInsightsController(AiInsightsService aiInsightsService) {
        this.aiInsightsService = aiInsightsService;
    }

    @PostMapping("/insights")
    public InsightsDto insights(
            @RequestBody(required = false) InsightsRequest body,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        int windowDays = (body != null && body.windowDays() != null && body.windowDays() > 0)
                ? body.windowDays()
                : 30;
        return aiInsightsService.generateInsights(currentUserId(), authHeader, windowDays);
    }

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String sub = jwt.getSubject();
            if (sub != null) return UUID.fromString(sub);
        }
        throw new IllegalStateException("No authenticated user");
    }
}
