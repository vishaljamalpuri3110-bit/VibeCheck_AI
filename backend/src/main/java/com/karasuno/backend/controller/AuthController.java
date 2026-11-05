package com.karasuno.backend.controller;

import com.karasuno.backend.dto.AuthRequest;
import com.karasuno.backend.dto.AuthResponse;
import com.karasuno.backend.dto.RegisterRequest;
import com.karasuno.backend.entity.User;
import com.karasuno.backend.repository.UserRepository;
import com.karasuno.backend.security.JwtTokenProvider;
import com.karasuno.backend.service.audit.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.LocalDateTime;

/**
 * REST controller for user authentication.
 * Public endpoints: POST /api/auth/register and POST /api/auth/login.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    /**
 * Registers a new user.
 * All public registrations are assigned ROLE_CONTENT_CREATOR.
 * Client-supplied roles are never trusted.
 */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest()
                    .body("Error: Username '" + request.getUsername() + "' is already taken.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                    .body("Error: Email '" + request.getEmail() + "' is already registered.");
        }

        String roles = "ROLE_CONTENT_CREATOR";

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .build();

        User saved = userRepository.save(user);
        auditService.recordAsync(saved.getId(), saved.getUsername(), "USER_REGISTERED",
                "User", saved.getId(), "Roles: " + roles, null);

        return ResponseEntity.ok("User registered successfully. ID: " + saved.getId());
    }

    /**
     * Authenticates a user and returns a signed JWT token with metadata.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request,
                                               HttpServletRequest httpRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(), request.getPassword()));

        String token = jwtTokenProvider.generateToken(authentication);
        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        auditService.recordAsync(user.getId(), user.getUsername(), "USER_LOGIN",
                "User", user.getId(), "Login success", httpRequest.getRemoteAddr());

        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getUsername(),
                user.getRoles(),
                jwtTokenProvider.getExpirationMs()));
    }
}
