package com.mrrg.backend.service;

import com.mrrg.backend.model.AccountActivationToken;
import com.mrrg.backend.model.User;
import com.mrrg.backend.model.UserRole;
import com.mrrg.backend.repository.AccountActivationTokenRepository;
import com.mrrg.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountActivationTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ActivationService activationService;

    @Test
    void validateActivationToken_shouldSucceed_forValidUnusedToken() {
        User user = new User("user@test.com", "", "User", UserRole.EMPLOYEE);
        user.setId(2L);

        AccountActivationToken token = new AccountActivationToken(
                "valid-token",
                user,
                System.currentTimeMillis() + 60_000
        );

        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        assertThatNoException().isThrownBy(() -> activationService.validateActivationToken("valid-token"));
        verify(tokenRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void validateActivationToken_shouldRejectInvalidToken() {
        when(tokenRepository.findByToken("missing-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activationService.validateActivationToken("missing-token"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid activation token.");
    }

    @Test
    void validateActivationToken_shouldRejectExpiredToken() {
        User user = new User("user@test.com", "", "User", UserRole.EMPLOYEE);
        AccountActivationToken token = new AccountActivationToken(
                "expired-token",
                user,
                System.currentTimeMillis() - 60_000
        );

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> activationService.validateActivationToken("expired-token"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Activation token has expired.");
    }

    @Test
    void validateActivationToken_shouldRejectUsedToken() {
        User user = new User("user@test.com", "hashed", "User", UserRole.EMPLOYEE);
        AccountActivationToken token = new AccountActivationToken(
                "used-token",
                user,
                System.currentTimeMillis() + 60_000
        );
        token.setUsedAt(System.currentTimeMillis() - 30_000);

        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> activationService.validateActivationToken("used-token"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Activation token has already been used.");
    }

    @Test
    void validateActivationToken_shouldNotActivateUser() {
        User user = new User("user@test.com", "", "User", UserRole.EMPLOYEE);
        user.setId(2L);

        AccountActivationToken token = new AccountActivationToken(
                "valid-token",
                user,
                System.currentTimeMillis() + 60_000
        );

        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        activationService.validateActivationToken("valid-token");

        verify(userRepository, never()).save(any());
        verify(tokenRepository, never()).save(any());
        assertThat(user.getEnabled()).isFalse();
    }

    @Test
    void validateActivationToken_shouldRejectBlankToken() {
        assertThatThrownBy(() -> activationService.validateActivationToken(" "))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
