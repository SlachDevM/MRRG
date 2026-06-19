package com.mrrg.backend.service;

import com.mrrg.backend.dto.UpdateUserRequest;
import com.mrrg.backend.dto.UserManagementResponse;
import com.mrrg.backend.model.AccountActivationToken;
import com.mrrg.backend.model.User;
import com.mrrg.backend.model.UserRole;
import com.mrrg.backend.model.UserStatus;
import com.mrrg.backend.repository.AccountActivationTokenRepository;
import com.mrrg.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserManagementService {

    private final UserRepository userRepository;
    private final AccountActivationTokenRepository tokenRepository;
    private final EmailService emailService;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int TOKEN_LENGTH = 32;

    public UserManagementService(
            UserRepository userRepository,
            AccountActivationTokenRepository tokenRepository,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    /**
     * Lists all users for Manager/Admin viewing.
     * Only MANAGER and ADMIN can call this.
     * Authorization must be enforced at the controller level.
     *
     * @return list of all users as UserManagementResponse DTOs
     */
    public List<UserManagementResponse> listAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserManagementResponse)
                .toList();
    }

    /**
     * Gets a single user by ID for detailed management.
     *
     * @param userId the user's ID
     * @return UserManagementResponse DTO
     */
    public UserManagementResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toUserManagementResponse(user);
    }

    /**
     * Restricts admin-only invite endpoint to ADMIN role only.
     * Admins can invite EMPLOYEE or MANAGER but NOT ADMIN.
     *
     * @param adminId the ID of the admin making the invitation
     * @param targetRole the role to invite (should be EMPLOYEE or MANAGER)
     * @throws ResponseStatusException if caller is not an admin or tries to invite ADMIN
     */
    public void validateAdminInvitationPermission(Long adminId, UserRole targetRole) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only ADMIN can invite users");
        }

        if (targetRole == UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot invite ADMIN users through this endpoint");
        }
    }

    /**
     * Updates a user's name and email. Email changes trigger notifications.
     * Only ADMINs can update users.
     *
     * @param userId the user to update
     * @param request the update request (name and email)
     * @param requestingAdminId the ID of the admin making the request
     * @return updated user as UserManagementResponse DTO
     */
    @Transactional
    public UserManagementResponse updateUser(Long userId, UpdateUserRequest request, Long requestingAdminId) {
        User admin = userRepository.findById(requestingAdminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only ADMIN can update users");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Validate input
        if ((request.getName() == null || request.getName().isBlank()) &&
                (request.getEmail() == null || request.getEmail().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one field (name or email) must be provided");
        }

        boolean emailChanged = false;
        String oldEmail = user.getEmail();

        // Update name if provided
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }

        // Update email if provided and different
        if (request.getEmail() != null && !request.getEmail().isBlank() && !request.getEmail().equals(oldEmail)) {
            // Check for duplicate email
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already in use");
            }

            user.setEmail(request.getEmail());
            emailChanged = true;
        }

        user.setUpdatedAt(System.currentTimeMillis());
        User updatedUser = userRepository.save(user);

        // Handle email change notifications
        if (emailChanged) {
            handleEmailChangeNotifications(updatedUser, oldEmail);
        }

        return toUserManagementResponse(updatedUser);
    }

    /**
     * Handles email change notifications for a user.
     * If user is PENDING_ACTIVATION, generates a new activation link.
     * If user is ACTIVE, sends a notification to both old and new email addresses.
     *
     * @param user the updated user with new email
     * @param oldEmail the previous email address
     */
    private void handleEmailChangeNotifications(User user, String oldEmail) {
        UserStatus status = computeStatus(user);

        if (status == UserStatus.PENDING_ACTIVATION) {
            // Invalidate old tokens for this user
            List<AccountActivationToken> oldTokens = tokenRepository.findUnusedByUserId(user.getId());
            oldTokens.forEach(t -> t.setUsedAt(System.currentTimeMillis()));
            tokenRepository.saveAll(oldTokens);

            // Generate and send new activation link to new email
            String newToken = generateSecureToken();
            AccountActivationToken activationToken = new AccountActivationToken(
                    newToken,
                    user,
                    System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24 hours
            );
            tokenRepository.save(activationToken);
            emailService.sendActivationEmail(user.getEmail(), newToken, user.getName());

            log.info("New activation link sent to user {} at new email address", user.getId());
        } else if (status == UserStatus.ACTIVE) {
            // Send notification to old email (optional but recommended)
            emailService.sendEmailChangeNotification(oldEmail, user.getEmail(), user.getName());
            log.info("Email change notification sent for user {}", user.getId());
        }
    }

    /**
     * Deactivates a user. Prevents the user from logging in.
     * Admins cannot deactivate their own account.
     *
     * For a PENDING_ACTIVATION user: invalidates activation tokens → status becomes DISABLED
     * For an ACTIVE user: sets enabled=false → status becomes DISABLED
     *
     * @param userId the user to deactivate
     * @param requestingAdminId the ID of the admin making the request
     */
    @Transactional
    public void deactivateUser(Long userId, Long requestingAdminId) {
        User admin = userRepository.findById(requestingAdminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only ADMIN can deactivate users");
        }

        if (userId.equals(requestingAdminId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot deactivate your own account");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Check current status to determine if we need to invalidate tokens
        UserStatus currentStatus = computeStatus(user);

        if (currentStatus == UserStatus.PENDING_ACTIVATION) {
            // Invalidate all unused activation tokens for this pending user
            // This ensures the old activation link will no longer work
            // and the status transitions from PENDING_ACTIVATION to DISABLED
            List<AccountActivationToken> pendingTokens = tokenRepository.findUnusedByUserId(userId);
            
            pendingTokens.forEach(t -> {
                t.setUsedAt(System.currentTimeMillis());
            });
            tokenRepository.saveAll(pendingTokens);
            
            log.info("Invalidated {} activation tokens for deactivated pending user {}", pendingTokens.size(), userId);
        }

        // Set enabled=false to prevent login
        // Status will be computed as:
        // - If PENDING was just deactivated: no valid tokens now → DISABLED
        // - If ACTIVE is deactivated: no tokens → DISABLED
        user.setEnabled(false);
        user.setUpdatedAt(System.currentTimeMillis());
        userRepository.save(user);

        log.info("User {} deactivated by admin {}. New status: {}", userId, requestingAdminId, computeStatus(user));
    }

    /**
     * Reactivates a user.
     *
     * For a DISABLED user who was previously ACTIVE:
     * - Sets enabled=true → status becomes ACTIVE
     *
     * For a DISABLED user who was previously PENDING (deactivated before ever activating):
     * - Do NOT set enabled=true
     * - Instead, reject and suggest using resend-activation
     * - This preserves the pending activation flow
     *
     * For an ACTIVE user:
     * - Throws error (already active)
     *
     * @param userId the user to reactivate
     * @param requestingAdminId the ID of the admin making the request
     */
    @Transactional
    public void reactivateUser(Long userId, Long requestingAdminId) {
        User admin = userRepository.findById(requestingAdminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only ADMIN can reactivate users");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UserStatus currentStatus = computeStatus(user);

        if (currentStatus == UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is already active");
        }

        if (currentStatus == UserStatus.PENDING_ACTIVATION) {
            // User never activated (still has valid token)
            // Do not silently activate them
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Cannot reactivate a pending user without activation. Use resend-activation instead.");
        }

        // currentStatus == DISABLED (enabled=false and no valid tokens)
        // This could be from:
        // 1. Admin deactivated an active user
        // 2. Admin deactivated a pending user (before reactivating)
        // 
        // For case 1: set enabled=true to restore to ACTIVE
        // For case 2: need to check if user has a password (was ever activated)
        //
        // If user has empty password, they never activated, so we need resend-activation
        // If user has a password, they were previously activated, so we can reactivate them

        if (!user.hasActivatedAccount()) {
            // User never set a password (never completed activation)
            // This is a deactivated-pending user
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "This user never completed activation. Use resend-activation instead.");
        }

        // User has a password, so they were previously active
        // Safe to reactivate
        user.setEnabled(true);
        user.setUpdatedAt(System.currentTimeMillis());
        userRepository.save(user);
        log.info("User {} reactivated by admin {}", userId, requestingAdminId);
    }

    /**
     * Resends activation link to a pending user.
     * Only allowed for users with PENDING_ACTIVATION status.
     *
     * @param userId the user to resend activation link to
     * @param requestingAdminId the ID of the admin making the request
     */
    @Transactional
    public void resendActivationLink(Long userId, Long requestingAdminId) {
        User admin = userRepository.findById(requestingAdminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));

        if (admin.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only ADMIN can resend activation links");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UserStatus currentStatus = computeStatus(user);
        boolean isNeverActivated = !user.hasActivatedAccount();

        // Allow resend for:
        // 1. PENDING_ACTIVATION users (normal case)
        // 2. DISABLED users who never activated (deactivated-pending users)
        if (currentStatus == UserStatus.PENDING_ACTIVATION) {
            // Normal pending user - resend activation
        } else if (currentStatus == UserStatus.DISABLED && isNeverActivated) {
            // Deactivated-pending user (never set password) - allow resend
        } else {
            // ACTIVE users or DISABLED users who already activated
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Activation link can only be resent for pending or never-activated users");
        }

        // Invalidate old tokens
        List<AccountActivationToken> oldTokens = tokenRepository.findUnusedByUserId(userId);
        oldTokens.forEach(t -> t.setUsedAt(System.currentTimeMillis()));
        tokenRepository.saveAll(oldTokens);

        // Generate and send new activation link
        String newToken = generateSecureToken();
        long expirationTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 hours
        AccountActivationToken activationToken = new AccountActivationToken(
                newToken,
                user,
                expirationTime
        );
        tokenRepository.save(activationToken);
        emailService.sendActivationEmail(user.getEmail(), newToken, user.getName());

        log.info("Activation link resent for user {} by admin {}", userId, requestingAdminId);
    }

    /**
     * Computes the user's activation status based on enabled flag and activation tokens.
     *
     * @param user the user to compute status for
     * @return UserStatus
     */
    public UserStatus computeStatus(User user) {
        if (user.getEnabled()) {
            return UserStatus.ACTIVE;
        }

        // Check if user has a valid (unused and not expired) activation token
        boolean hasPendingToken = tokenRepository.hasValidTokenByUserId(user.getId());

        return hasPendingToken ? UserStatus.PENDING_ACTIVATION : UserStatus.DISABLED;
    }

    /**
     * Converts a User entity to a UserManagementResponse DTO.
     *
     * @param user the user to convert
     * @return UserManagementResponse DTO
     */
    private UserManagementResponse toUserManagementResponse(User user) {
        UserStatus status = computeStatus(user);
        return new UserManagementResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                status,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    /**
     * Generates a secure random token for account activation.
     *
     * @return base64-encoded secure token
     */
    private String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
