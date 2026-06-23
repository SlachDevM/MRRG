package com.mrrg.backend.bootstrap;

import com.mrrg.backend.model.User;
import com.mrrg.backend.model.UserRole;
import com.mrrg.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitialAdminBootstrapTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationArguments applicationArguments;

    @Test
    void run_createsInitialAdminWhenNoAdminExists() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(userRepository.findByEmail("admin@mrrg.local")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("test")).thenReturn("encoded-password");

        InitialAdminBootstrap bootstrap = new InitialAdminBootstrap(
                userRepository,
                passwordEncoder,
                "admin@mrrg.local",
                "test",
                "Administrator"
        );

        bootstrap.run(applicationArguments);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("admin@mrrg.local");
        assertThat(savedUser.getName()).isEqualTo("Administrator");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(savedUser.getEnabled()).isTrue();
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        verify(passwordEncoder).encode("test");
    }

    @Test
    void run_skipsWhenAdminAlreadyExists() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);

        InitialAdminBootstrap bootstrap = new InitialAdminBootstrap(
                userRepository,
                passwordEncoder,
                "admin@mrrg.local",
                "test",
                "Administrator"
        );

        bootstrap.run(applicationArguments);

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void run_skipsWhenCredentialsAreMissing() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);

        InitialAdminBootstrap bootstrap = new InitialAdminBootstrap(
                userRepository,
                passwordEncoder,
                " ",
                "test",
                "Administrator"
        );

        bootstrap.run(applicationArguments);

        verify(userRepository, never()).save(any());
    }

    @Test
    void run_skipsWhenEmailAlreadyExists() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(userRepository.findByEmail("admin@mrrg.local"))
                .thenReturn(Optional.of(new User("admin@mrrg.local", "hash", "Existing", UserRole.MANAGER)));

        InitialAdminBootstrap bootstrap = new InitialAdminBootstrap(
                userRepository,
                passwordEncoder,
                "Admin@MRRG.local",
                "test",
                "Administrator"
        );

        bootstrap.run(applicationArguments);

        verify(userRepository).findByEmail("admin@mrrg.local");
        verify(userRepository, never()).save(any());
    }

    @Test
    void run_isIdempotentAcrossRestarts() {
        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);

        InitialAdminBootstrap bootstrap = new InitialAdminBootstrap(
                userRepository,
                passwordEncoder,
                "admin@mrrg.local",
                "test",
                "Administrator"
        );

        bootstrap.run(applicationArguments);
        bootstrap.run(applicationArguments);

        verify(userRepository, never()).save(any());
    }
}
