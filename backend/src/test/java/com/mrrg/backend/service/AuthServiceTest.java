package com.mrrg.backend.service;

import com.mrrg.backend.dto.LoginRequest;
import com.mrrg.backend.dto.LoginResponse;
import com.mrrg.backend.dto.RegisterRequest;
import com.mrrg.backend.model.User;
import com.mrrg.backend.model.UserRole;
import com.mrrg.backend.repository.UserRepository;
import com.mrrg.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() {
        User user = new User("manager@test.com", "encoded-password", "Manager", UserRole.MANAGER);
        user.setId(1L);
        user.setEnabled(true);

        when(userRepository.findByEmail("manager@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(tokenProvider.generateToken(1L, "manager@test.com")).thenReturn("jwt-token");

        LoginResponse response = authService.login(
                new LoginRequest("manager@test.com", "password")
        );

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("manager@test.com");
        assertThat(response.getName()).isEqualTo("Manager");
        assertThat(response.getRole()).isEqualTo(UserRole.MANAGER);
        assertThat(response.getToken()).isEqualTo("jwt-token");
    }

    @Test
    void login_shouldThrowForbidden_whenAccountNotActivated() {
        User user = new User("inactive@test.com", "encoded-password", "Inactive User", UserRole.EMPLOYEE);
        user.setId(3L);
        user.setEnabled(false);

        when(userRepository.findByEmail("inactive@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("inactive@test.com", "password")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403")
                .hasMessageContaining("not activated");

        verify(tokenProvider, never()).generateToken(anyLong(), anyString());
    }

    @Test
    void login_shouldThrowUnauthorized_whenPasswordIsInvalid() {
        User user = new User("worker@test.com", "encoded-password", "Worker", UserRole.EMPLOYEE);
        user.setId(2L);
        user.setEnabled(true);

        when(userRepository.findByEmail("worker@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("worker@test.com", "wrong-password")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");

        verify(tokenProvider, never()).generateToken(anyLong(), anyString());
    }

    @Test
    void register_shouldCreateEmployeeByDefault_whenRoleIsNull() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@test.com");
        request.setPassword("password");
        request.setName("New User");
        request.setRole(null);

        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        when(tokenProvider.generateToken(10L, "new@test.com")).thenReturn("jwt-token");

        LoginResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("new@test.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getName()).isEqualTo("New User");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.EMPLOYEE);
        // New registrations via public endpoint must be activated via email (enabled = false)
        assertThat(savedUser.getEnabled()).isFalse();

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getRole()).isEqualTo(UserRole.EMPLOYEE);
    }

    @Test
    void register_shouldThrowBadRequest_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@test.com");
        request.setPassword("password");
        request.setName("Existing User");

        when(userRepository.findByEmail("existing@test.com"))
                .thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");

        verify(userRepository, never()).save(any(User.class));
    }
}