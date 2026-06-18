package com.mrrg.backend.controller;

import com.mrrg.backend.dto.UserSummary;
import com.mrrg.backend.dto.FcmTokenRequest;
import com.mrrg.backend.model.User;
import com.mrrg.backend.security.JwtAuthenticationToken;
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

    public UserController(UserService userService) {
        this.userService = userService;
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
    public ResponseEntity<User> updateFcmToken(
            @RequestBody FcmTokenRequest request,
            Authentication authentication) {
        Long userId = ((JwtAuthenticationToken) authentication).getUserId();
        
        if (request.getToken() == null || request.getToken().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        
        User updatedUser = userService.updateFcmToken(userId, request.getToken());
        return ResponseEntity.ok(updatedUser);
    }
}
