package com.mrrg.backend.service;

import com.mrrg.backend.model.AccountActivationToken;
import com.mrrg.backend.model.User;
import com.mrrg.backend.model.UserRole;
import com.mrrg.backend.repository.AccountActivationTokenRepository;
import com.mrrg.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @Test
    void createEmployeeInvitation_shouldStoreUserReferenceOnToken() {
        User creator = new User("admin@test.com", "password", "Admin", UserRole.ADMIN);
        creator.setId(1L);

        User savedUser = new User("new@test.com", "", "New User", UserRole.EMPLOYEE);
        savedUser.setId(10L);
        savedUser.setEnabled(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenRepository.save(any(AccountActivationToken.class))).thenAnswer(i -> i.getArguments()[0]);

        activationService.createEmployeeInvitation(1L, "New User", "new@test.com", UserRole.EMPLOYEE);

        ArgumentCaptor<AccountActivationToken> tokenCaptor = ArgumentCaptor.forClass(AccountActivationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());

        AccountActivationToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isSameAs(savedUser);
        assertThat(savedToken.getUserId()).isEqualTo(10L);
        assertThat(savedToken.getToken()).isNotBlank();
        verify(emailService).sendActivationEmail(eq("new@test.com"), eq(savedToken.getToken()), eq("New User"));
    }

    @Test
    void activateAccount_shouldActivateUserAndMarkTokenUsed() {
        User user = new User("user@test.com", "", "User", UserRole.EMPLOYEE);
        user.setId(2L);
        user.setEnabled(false);

        AccountActivationToken token = new AccountActivationToken(
                "valid-token",
                user,
                System.currentTimeMillis() + 60_000
        );

        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-password");
        when(userRepository.save(user)).thenReturn(user);
        when(tokenRepository.save(token)).thenReturn(token);

        User activated = activationService.activateAccount("valid-token", "new-password");

        assertThat(activated.getEnabled()).isTrue();
        assertThat(activated.getPassword()).isEqualTo("encoded-password");
        assertThat(token.isUsed()).isTrue();
        verify(tokenRepository).save(token);
    }

    @Test
    void activateAccount_shouldRejectUsedToken() {
        User user = new User("user@test.com", "hashed", "User", UserRole.EMPLOYEE);
        AccountActivationToken token = new AccountActivationToken(
                "used-token",
                user,
                System.currentTimeMillis() + 60_000
        );
        token.setUsedAt(System.currentTimeMillis() - 30_000);

        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> activationService.activateAccount("used-token", "password"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Activation token has already been used.");
    }

    @Test
    void activateAccount_shouldRejectExpiredToken() {
        User user = new User("user@test.com", "", "User", UserRole.EMPLOYEE);
        AccountActivationToken token = new AccountActivationToken(
                "expired-token",
                user,
                System.currentTimeMillis() - 60_000
        );

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> activationService.activateAccount("expired-token", "password"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Activation token has expired.");
    }
}
