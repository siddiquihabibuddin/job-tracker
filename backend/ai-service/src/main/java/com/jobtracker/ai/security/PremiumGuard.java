package com.jobtracker.ai.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

/**
 * Utility for server-side premium-tier enforcement.
 *
 * <p>Call {@link #requirePremium(Authentication)} as the first statement in any
 * controller method that must be restricted to PREMIUM users. This is intentionally
 * an explicit call (not an annotation) so it is immediately visible when reading
 * the method body and easy to search for across the codebase.
 *
 * <p>Duplicated per project convention (no shared library between services) —
 * mirrors {@code com.jobtracker.apps.security.PremiumGuard}.
 *
 * <p><strong>Freshness note:</strong> the tier value is read from the JWT claim,
 * not from the database. Tokens are valid for 24 hours. A user who upgrades
 * receives a fresh token immediately (via {@code POST /v1/billing/checkout}), so
 * they get premium access instantly. A user who is manually downgraded will retain
 * PREMIUM access for at most 24 hours until their current token expires — this is
 * an acceptable trade-off for the mock-billing demo and avoids per-request DB
 * lookups.
 */
public final class PremiumGuard {

    private PremiumGuard() {}

    /**
     * Throws {@link ResponseStatusException} with HTTP 403 if the caller is not
     * authenticated, or HTTP 402 (Payment Required) if the caller's tier claim is
     * not {@code "PREMIUM"}.
     *
     * @param auth the {@link Authentication} from the controller method parameter
     */
    public static void requirePremium(Authentication auth) {
        if (!(auth != null && auth.getPrincipal() instanceof Jwt jwt)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (!"PREMIUM".equals(jwt.getClaimAsString("tier"))) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "premium_required");
        }
    }
}
