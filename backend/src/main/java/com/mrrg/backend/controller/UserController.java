package com.mrrg.backend.controller;

import com.mrrg.backend.dto.*;
import com.mrrg.backend.model.User;
import com.mrrg.backend.security.JwtAuthenticationToken;
import com.mrrg.backend.service.ActivationService;
import com.mrrg.backend.service.UserManagementService;
import com.mrrg.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    private final UserService userService;
    private final ActivationService activationService;
    private final UserManagementService userManagementService;

    public UserController(
            UserService userService,
            ActivationService activationService,
            UserManagementService userManagementService) {
        this.userService = userService;
        this.activationService = activationService;
        this.userManagementService = userManagementService;
    }

    /**
     * Lists all users for MANAGER and ADMIN.
     * Managers see read-only list.
     * Admins see editable list.
     */
    @GetMapping
    public ResponseEntity<List<UserManagementResponse>> listUsers(Authentication authentication) {
        Long userId = ((JwtAuthenticationToken) authentication).getUserId();
        if (!userService.isManagerOrAdmin(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(userManagementService.listAllUsers());
    }

    /**
     * Gets a single user by ID.
     * Only MANAGER and ADMIN can access.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserManagementResponse> getUser(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = ((JwtAuthenticationToken) authentication).getUserId();
        if (!userService.isManagerOrAdmin(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(userManagementService.getUserById(id));
    }

    @PostMapping("/invitations")
    public ResponseEntity<UserProfileResponse> createEmployeeInvitation(
            @RequestBody CreateEmployeeRequest request,
            Authentication authentication) {
        Long userId = ((JwtAuthenticationToken) authentication).getUserId();
        
        // Validate admin permission
        userManagementService.validateAdminInvitationPermission(userId, request.getRole());
        
        User invitedUser = activationService.createEmployeeInvitation(
                userId,
                request.getName(),
                request.getEmail(),
                request.getRole()
        );

        UserProfileResponse response = new UserProfileResponse(
                invitedUser.getId(),
                invitedUser.getName(),
                invitedUser.getEmail(),
                invitedUser.getRole()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates a user's name and/or email.
     * Only ADMIN can update users.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserManagementResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        Long adminId = ((JwtAuthenticationToken) authentication).getUserId();
        UserManagementResponse updatedUser = userManagementService.updateUser(id, request, adminId);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Deactivates a user. User will not be able to log in.
     * Only ADMIN can deactivate users.
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(
            @PathVariable Long id,
            Authentication authentication) {
        Long adminId = ((JwtAuthenticationToken) authentication).getUserId();
        userManagementService.deactivateUser(id, adminId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivates a user. User will be able to log in again.
     * Only ADMIN can reactivate users.
     */
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivateUser(
            @PathVariable Long id,
            Authentication authentication) {
        Long adminId = ((JwtAuthenticationToken) authentication).getUserId();
        userManagementService.reactivateUser(id, adminId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Resends activation link to a pending user.
     * Only ADMIN can resend activation links.
     * Only works for users with PENDING_ACTIVATION status.
     */
    @PostMapping("/{id}/resend-activation")
    public ResponseEntity<Void> resendActivationLink(
            @PathVariable Long id,
            Authentication authentication) {
        Long adminId = ((JwtAuthenticationToken) authentication).getUserId();
        userManagementService.resendActivationLink(id, adminId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        Long userId = ((JwtAuthenticationToken) authentication).getUserId();
        User user = userService.getById(userId);
        UserProfileResponse response = new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/workers")
    public ResponseEntity<List<UserSummary>> getWorkers(Authentication authentication) {
        Long userId = ((JwtAuthenticationToken) authentication).getUserId();
        if (!userService.isManagerOrAdmin(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(userManagementService.getAssignableWorkers());
    }

    @PutMapping("/me/fcm-token")
    public ResponseEntity<Void> updateFcmToken(
            @RequestBody FcmTokenRequest request,
            Authentication authentication) {
        Long userId = ((JwtAuthenticationToken) authentication).getUserId();
        
        if (request.getToken() == null || request.getToken().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        
        userService.updateFcmToken(userId, request.getToken());
        return ResponseEntity.noContent().build();
    }
}
