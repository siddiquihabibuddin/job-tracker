package com.jobtracker.apps.api;

import com.jobtracker.apps.api.dto.AuthResponse;
import com.jobtracker.apps.api.dto.RegisterRequest;
import com.jobtracker.apps.domain.model.User;
import com.jobtracker.apps.domain.repo.UserRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final String jwtSecret;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(
            @org.springframework.beans.factory.annotation.Value("${security.jwt.secret}") String jwtSecret,
            UserRepository userRepository,
            BCryptPasswordEncoder passwordEncoder) {
        this.jwtSecret = jwtSecret;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    @PostMapping("/token")
    public ResponseEntity<?> token(@Valid @RequestBody LoginRequest req) {
        String normalizedEmail = req.email().toLowerCase().strip();
        Optional<User> found = userRepository.findByEmail(normalizedEmail)
                .filter(u -> u.isActive()
                        && u.getPasswordHash() != null
                        && passwordEncoder.matches(req.password(), u.getPasswordHash()));
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
        return buildTokenResponse(found.get(), HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        String normalizedEmail = req.email().toLowerCase().strip();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already registered"));
        }

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setDisplayName(req.displayName());
        user.setActive(true);
        userRepository.save(user);

        return buildTokenResponse(user, HttpStatus.CREATED);
    }

    private ResponseEntity<?> buildTokenResponse(User user, HttpStatus status) {
        try {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + 24L * 60 * 60 * 1000);

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(user.getId().toString())
                    .claim("email", user.getEmail())
                    .issuer("job-tracker")
                    .issueTime(now)
                    .expirationTime(expiry)
                    .build();

            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(jwtSecret.getBytes()));

            AuthResponse body = new AuthResponse(
                    jwt.serialize(),
                    user.getEmail(),
                    user.getId().toString(),
                    user.getDisplayName()
            );
            return ResponseEntity.status(status).body(body);
        } catch (JOSEException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Token generation failed"));
        }
    }
}
