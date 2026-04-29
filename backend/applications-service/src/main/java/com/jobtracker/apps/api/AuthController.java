package com.jobtracker.apps.api;

import com.jobtracker.apps.api.dto.AuthResponse;
import com.jobtracker.apps.api.dto.RegisterRequest;
import com.jobtracker.apps.domain.model.User;
import com.jobtracker.apps.domain.model.UserTier;
import com.jobtracker.apps.domain.repo.UserRepository;
import com.nimbusds.jose.JOSEException;
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

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final JwtIssuer jwtIssuer;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(
            JwtIssuer jwtIssuer,
            UserRepository userRepository,
            BCryptPasswordEncoder passwordEncoder) {
        this.jwtIssuer = jwtIssuer;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    // Owner account always gets PREMIUM tier on any fresh deploy.
    private static final String OWNER_EMAIL = "saifz7@gmail.com";

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
        User user = found.get();
        if (OWNER_EMAIL.equals(user.getEmail()) && user.getTier() != UserTier.PREMIUM) {
            user.setTier(UserTier.PREMIUM);
            userRepository.save(user);
        }
        return buildTokenResponse(user, HttpStatus.OK);
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
        if (OWNER_EMAIL.equals(normalizedEmail)) {
            user.setTier(UserTier.PREMIUM);
        }
        userRepository.save(user);

        return buildTokenResponse(user, HttpStatus.CREATED);
    }

    ResponseEntity<?> buildTokenResponse(User user, HttpStatus status) {
        try {
            AuthResponse body = jwtIssuer.issue(user);
            return ResponseEntity.status(status).body(body);
        } catch (JOSEException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Token generation failed"));
        }
    }
}
