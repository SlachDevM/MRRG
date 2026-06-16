package com.mrrg.backend.service;

import com.mrrg.backend.dto.UserSummary;
import com.mrrg.backend.model.User;
import com.mrrg.backend.model.UserRole;
import com.mrrg.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getById_shouldReturnUser_whenUserExists() {
        User user = new User("manager@test.com", "password", "Manager", UserRole.MANAGER);
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.getById(1L);

        assertThat(result).isEqualTo(user);
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRole()).isEqualTo(UserRole.MANAGER);
    }

    @Test
    void getById_shouldThrowNotFound_whenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void getWorkers_shouldReturnEmployeesAndManagersAsUserSummaries() {
        User employee = new User("employee@test.com", "password", "Employee", UserRole.EMPLOYEE);
        employee.setId(1L);

        User manager = new User("manager@test.com", "password", "Manager", UserRole.MANAGER);
        manager.setId(2L);

        when(userRepository.findByRoleInOrderByNameAsc(
                List.of(UserRole.EMPLOYEE, UserRole.MANAGER)
        )).thenReturn(List.of(employee, manager));

        List<UserSummary> result = userService.getWorkers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Employee");
        assertThat(result.get(1).getName()).isEqualTo("Manager");

        verify(userRepository).findByRoleInOrderByNameAsc(
                List.of(UserRole.EMPLOYEE, UserRole.MANAGER)
        );
    }

    @Test
    void isManagerOrAdmin_shouldReturnTrue_whenUserIsManager() {
        User manager = new User("manager@test.com", "password", "Manager", UserRole.MANAGER);
        manager.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));

        boolean result = userService.isManagerOrAdmin(1L);

        assertThat(result).isTrue();
    }

    @Test
    void isManagerOrAdmin_shouldReturnTrue_whenUserIsAdmin() {
        User admin = new User("admin@test.com", "password", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        boolean result = userService.isManagerOrAdmin(1L);

        assertThat(result).isTrue();
    }

    @Test
    void isManagerOrAdmin_shouldReturnFalse_whenUserIsEmployee() {
        User employee = new User("employee@test.com", "password", "Employee", UserRole.EMPLOYEE);
        employee.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));

        boolean result = userService.isManagerOrAdmin(1L);

        assertThat(result).isFalse();
    }

    @Test
    void isAdmin_shouldReturnTrue_whenUserIsAdmin() {
        User admin = new User("admin@test.com", "password", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        boolean result = userService.isAdmin(1L);

        assertThat(result).isTrue();
    }

    @Test
    void isAdmin_shouldReturnFalse_whenUserIsManager() {
        User manager = new User("manager@test.com", "password", "Manager", UserRole.MANAGER);
        manager.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));

        boolean result = userService.isAdmin(1L);

        assertThat(result).isFalse();
    }

    @Test
    void findByName_shouldReturnMatchingUsers() {
        User worker = new User("worker@test.com", "password", "John Worker", UserRole.EMPLOYEE);
        worker.setId(1L);

        when(userRepository.findByName("John Worker")).thenReturn(List.of(worker));

        List<User> result = userService.findByName("John Worker");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("John Worker");

        verify(userRepository).findByName("John Worker");
    }
}