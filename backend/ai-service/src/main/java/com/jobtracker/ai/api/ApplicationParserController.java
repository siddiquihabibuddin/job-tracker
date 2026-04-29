package com.jobtracker.ai.api;

import com.jobtracker.ai.api.dto.ParseApplicationRequest;
import com.jobtracker.ai.api.dto.ParsedApplicationDto;
import com.jobtracker.ai.security.PremiumGuard;
import com.jobtracker.ai.service.ApplicationParserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/ai")
public class ApplicationParserController {

    private static final Logger log = LoggerFactory.getLogger(ApplicationParserController.class);

    private final ApplicationParserService applicationParserService;

    public ApplicationParserController(ApplicationParserService applicationParserService) {
        this.applicationParserService = applicationParserService;
    }

    @PostMapping("/applications/parse")
    public ParsedApplicationDto parse(Authentication auth, @Valid @RequestBody ParseApplicationRequest request) {
        PremiumGuard.requirePremium(auth);
        try {
            return applicationParserService.parse(request.description());
        } catch (Exception e) {
            log.error("Failed to parse application description", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to parse description");
        }
    }

}
