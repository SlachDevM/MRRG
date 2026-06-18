package com.mrrg.backend.service;

import com.mrrg.backend.dto.UserSummary;
import com.mrrg.backend.model.User;
import com.mrrg.backend.model.UserRole;
import com.mrrg.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public List<UserSummary> getWorkers() {

        return userRepository
                .findByRoleInOrderByNameAsc(
                        Arrays.asList(
                                UserRole.EMPLOYEE,
                                UserRole.MANAGER
                        )
                )
                .stream()
                .map(UserSummary::new)
                .toList();
    }

    public boolean isManagerOrAdmin(Long userId) {

        User user = getById(userId);

        return user.getRole() == UserRole.ADMIN
                || user.getRole() == UserRole.MANAGER;
    }

    public boolean isAdmin(Long userId) {

        return getById(userId).getRole() == UserRole.ADMIN;
    }

    public List<User> findByName(String name) {
        return userRepository.findByName(name);
    }

    public User updateFcmToken(Long userId, String fcmToken) {
        User user = getById(userId);
        user.setFcmToken(fcmToken);
        user.setUpdatedAt(System.currentTimeMillis());
        return userRepository.save(user);
    }
}