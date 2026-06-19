package com.mrrg.backend.controller;

import com.mrrg.backend.dto.CreateEmployeeRequest;
import com.mrrg.backend.dto.UserSummary;
import com.mrrg.backend.dto.FcmTokenRequest;
import com.mrrg.backend.dto.UserProfileResponse;
import com.mrrg.backend.model.User;
import com.mrrg.backend.security.JwtAuthenticationToken;
import com.mrrg.backend.service.ActivationService;
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

    public UserController(UserService userService, ActivationService activationService) {
        this.userService = userService;
        this.activationService = activationService;
    }

    @PostMapping("/invitations")
    public ResponseEntity<UserProfileResponse> createEmployeeInvitation(
            @RequestBody CreateEmployeeRequest request,
            Authentication authentication) {
        Long userId = ((JwtAuthenticationToken) authentication).getUserId();
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
        return ResponseEntity.ok(userService.getWorkers());
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
