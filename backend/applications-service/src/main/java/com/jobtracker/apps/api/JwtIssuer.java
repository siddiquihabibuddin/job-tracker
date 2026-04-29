package com.jobtracker.apps.api;

import com.jobtracker.apps.api.dto.AuthResponse;
import com.jobtracker.apps.domain.model.User;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Package-visible helper that issues HS256 JWTs and assembles AuthResponse bodies.
 * Shared between {@link AuthController} and {@link BillingController} so that
 * token issuance logic stays in one place.
 */
@Component
class JwtIssuer {

    private final String jwtSecret;

    JwtIssuer(@Value("${security.jwt.secret}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    /**
     * Issues a 24-hour HS256 token for the given user and returns a complete
     * {@link AuthResponse} suitable for returning directly to the caller.
     */
    AuthResponse issue(User user) throws JOSEException {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 24L * 60 * 60 * 1000);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("tier", user.getTier().name())
                .issuer("job-tracker")
                .issueTime(now)
                .expirationTime(expiry)
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(jwtSecret.getBytes()));

        return new AuthResponse(
                jwt.serialize(),
                user.getEmail(),
                user.getId().toString(),
                user.getDisplayName(),
                user.getTier().name()
        );
    }
}
