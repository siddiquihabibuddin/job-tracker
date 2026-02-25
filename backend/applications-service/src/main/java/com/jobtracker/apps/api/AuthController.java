package com.jobtracker.apps.api;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private static final String DEMO_USER_ID = "00000000-0000-0000-0000-000000000001";
    private static final String DEMO_EMAIL = "demo@example.com";
    private static final String DEMO_PASSWORD = "demo";

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    public record LoginRequest(String email, String password) {}
    public record TokenResponse(String token, String email, String userId) {}

    @PostMapping("/token")
    public ResponseEntity<?> token(@RequestBody LoginRequest req) {
        if (!DEMO_EMAIL.equals(req.email()) || !DEMO_PASSWORD.equals(req.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }

        try {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + 24L * 60 * 60 * 1000); // 24h

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(DEMO_USER_ID)
                    .claim("email", DEMO_EMAIL)
                    .issuer("job-tracker")
                    .issueTime(now)
                    .expirationTime(expiry)
                    .build();

            SignedJWT jwt = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claims
            );
            jwt.sign(new MACSigner(jwtSecret.getBytes()));

            return ResponseEntity.ok(new TokenResponse(jwt.serialize(), DEMO_EMAIL, DEMO_USER_ID));
        } catch (JOSEException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Token generation failed"));
        }
    }
}
