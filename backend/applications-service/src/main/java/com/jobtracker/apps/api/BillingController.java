package com.jobtracker.apps.api;

import com.jobtracker.apps.api.dto.AuthResponse;
import com.jobtracker.apps.domain.model.User;
import com.jobtracker.apps.domain.model.UserTier;
import com.jobtracker.apps.domain.repo.UserRepository;
import com.nimbusds.jose.JOSEException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.Year;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/billing")
public class BillingController {

    private static final Logger log = LoggerFactory.getLogger(BillingController.class);

    private final UserRepository userRepository;
    private final JwtIssuer jwtIssuer;

    public BillingController(UserRepository userRepository, JwtIssuer jwtIssuer) {
        this.userRepository = userRepository;
        this.jwtIssuer = jwtIssuer;
    }

    /**
     * Card data DTO — accepted only for shape validation; never persisted or logged.
     */
    public record CheckoutRequest(
            @NotBlank @Pattern(regexp = "\\d{13,19}", message = "cardNumber must be 13-19 digits") String cardNumber,
            @Min(1) @Max(12) int expMonth,
            int expYear,
            @NotBlank @Pattern(regexp = "\\d{3,4}", message = "cvc must be 3 or 4 digits") String cvc,
            @NotBlank String nameOnCard) {
        // Compact constructor: strip spaces/dashes from card data before Bean Validation runs.
        public CheckoutRequest {
            if (cardNumber != null) cardNumber = cardNumber.replaceAll("[\\s\\-]", "");
            if (cvc != null) cvc = cvc.strip();
            if (nameOnCard != null) nameOnCard = nameOnCard.strip();
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@Valid @RequestBody CheckoutRequest req) {
        int currentYear = Year.now().getValue();
        if (req.expYear() < currentYear || req.expYear() > currentYear + 20) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Validation failed",
                            "details", "expYear: must be between " + currentYear + " and " + (currentYear + 20)));
        }

        if (!luhnCheck(req.cardNumber())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Validation failed", "details", "cardNumber: invalid card number"));
        }

        UUID userId = currentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setTier(UserTier.PREMIUM);
        user.setTierUpdatedAt(Instant.now());
        userRepository.save(user);

        // Never log card details — only log the opaque user id
        log.info("mock charge ok user={}", userId);

        try {
            AuthResponse body = jwtIssuer.issue(user);
            return ResponseEntity.ok(body);
        } catch (JOSEException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Token generation failed"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        UUID userId = currentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(Map.of(
                "tier", user.getTier().name(),
                "tierUpdatedAt", user.getTierUpdatedAt() != null ? user.getTierUpdatedAt().toString() : ""
        ));
    }

    // --- helpers ---

    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String sub = jwt.getSubject();
            if (sub != null) return UUID.fromString(sub);
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
    }

    /**
     * Luhn algorithm check. Returns {@code true} if the card number passes Luhn validation.
     */
    private static boolean luhnCheck(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            char c = number.charAt(i);
            if (c < '0' || c > '9') return false;
            int n = c - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }
}
