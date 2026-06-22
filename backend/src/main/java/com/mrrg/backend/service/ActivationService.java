package com.mrrg.backend.service;

import com.mrrg.backend.model.AccountActivationToken;
import com.mrrg.backend.model.User;
import com.mrrg.backend.model.UserRole;
import com.mrrg.backend.repository.AccountActivationTokenRepository;
import com.mrrg.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Base64;

@Service
@Slf4j
public class ActivationService {

    private final UserRepository userRepository;
    private final AccountActivationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.activation-token-expiration-hours:24}")
    private long tokenExpirationHours;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int TOKEN_LENGTH = 32;

    public ActivationService(
            UserRepository userRepository,
            AccountActivationTokenRepository tokenRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates an employee invitation and sends an activation email.
     * Only managers and admins can invite employees.
     *
     * @param creatorId the ID of the manager/admin creating the invitation
     * @param name the employee's name
     * @param email the employee's email
     * @param requestedRole the requested role (validated based on creator permissions)
     * @return the created (but inactive) user
     */
    @Transactional
    public User createEmployeeInvitation(Long creatorId, String name, String email, UserRole requestedRole) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Creator not found"));

        // Normalize email
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";

        // Validate creator permissions
        validateCreatorPermissions(creator, requestedRole);

        // Check for duplicate email
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A user already exists with this email.");
        }

        // Validate inputs
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name cannot be blank");
        }
        if (normalizedEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email cannot be blank");
        }

        // Create inactive user
        User newUser = new User(normalizedEmail, "", name, requestedRole);
        newUser.setEnabled(false);
        User savedUser = userRepository.save(newUser);

        // Generate and save activation token
        String token = generateSecureToken();
        long expirationTime = System.currentTimeMillis() + (tokenExpirationHours * 60 * 60 * 1000);
        AccountActivationToken activationToken = new AccountActivationToken(token, savedUser, expirationTime);
        tokenRepository.save(activationToken);

        // Send activation email
        emailService.sendActivationEmail(normalizedEmail, token, name);

        log.info("Employee invitation created for email: {} by user: {}", normalizedEmail, creator.getId());
        return savedUser;
    }

    /**
     * Validates an activation token without activating the account or modifying token state.
     *
     * @param token the activation token to validate
     */
    public void validateActivationToken(String token) {
        requireUsableActivationToken(token);
    }

    /**
     * Activates a user account with a valid token and password.
     *
     * @param token the activation token
     * @param password the user's chosen password
     * @return the activated user
     */
    @Transactional
    public User activateAccount(String token, String password) {
        // Validate inputs
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }

        AccountActivationToken activationToken = requireUsableActivationToken(token);

        // Activate user
        User user = activationToken.getUser();
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);
        user.setUpdatedAt(System.currentTimeMillis());
        User activatedUser = userRepository.save(user);

        // Mark token as used
        activationToken.setUsedAt(System.currentTimeMillis());
        tokenRepository.save(activationToken);

        log.info("Account activated for user: {}", user.getId());
        return activatedUser;
    }

    private AccountActivationToken requireUsableActivationToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid activation token.");
        }

        AccountActivationToken activationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid activation token."));

        if (activationToken.isExpired()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Activation token has expired.");
        }
        if (activationToken.isUsed()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Activation token has already been used.");
        }

        return activationToken;
    }

    private void validateCreatorPermissions(User creator, UserRole requestedRole) {
        boolean isAdmin = creator.getRole() == UserRole.ADMIN;
        boolean isManager = creator.getRole() == UserRole.MANAGER;

        if (!isAdmin && !isManager) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only managers and admins can create employee invitations");
        }

        // Managers can only create EMPLOYEE accounts
        if (isManager && requestedRole != UserRole.EMPLOYEE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Managers can only create employee accounts");
        }

        // Admins can create EMPLOYEE or MANAGER accounts, but not other ADMIN accounts
        if (isAdmin && requestedRole == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot create admin accounts via invitation");
        }
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
