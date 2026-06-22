package com.mrrg.backend.controller;

import com.mrrg.backend.dto.ActivateAccountRequest;
import com.mrrg.backend.dto.LoginRequest;
import com.mrrg.backend.dto.LoginResponse;
import com.mrrg.backend.dto.RegisterRequest;
import com.mrrg.backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private final AuthService authService;
    private final boolean registerEndpointEnabled;

    public AuthController(AuthService authService, RegisterEndpointProperty registerEndpointProperty) {
        this.authService = authService;
        this.registerEndpointEnabled = registerEndpointProperty.isEnabled();
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody RegisterRequest request) {
        if (!registerEndpointEnabled) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Public registration is disabled. Contact an administrator to request account activation."
            );
        }
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/activate-account")
    public ResponseEntity<LoginResponse> activateAccount(@RequestBody ActivateAccountRequest request) {
        return ResponseEntity.ok(authService.activateAccount(request.getToken(), request.getPassword()));
    }

    @GetMapping("/activation-token/validate")
    public ResponseEntity<Void> validateActivationToken(@RequestParam("token") String token) {
        authService.validateActivationToken(token);
        return ResponseEntity.noContent().build();
    }

    /**
     * Determines if public registration endpoint is enabled based on active profile.
     * Registration is only enabled in development profiles (dev, development, local).
     */
    @org.springframework.stereotype.Component
    public static class RegisterEndpointProperty {
        private final String activeProfile;

        public RegisterEndpointProperty(@org.springframework.beans.factory.annotation.Value("${spring.profiles.active:dev}") String activeProfile) {
            this.activeProfile = activeProfile;
        }

        public boolean isEnabled() {
            return "dev".equalsIgnoreCase(activeProfile) 
                    || "development".equalsIgnoreCase(activeProfile) 
                    || "local".equalsIgnoreCase(activeProfile);
        }
    }
}
