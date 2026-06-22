package com.mrrg.backend.service;

import com.mrrg.backend.dto.UpdateUserRequest;
import com.mrrg.backend.dto.UserManagementResponse;
import com.mrrg.backend.dto.UserSummary;
import com.mrrg.backend.model.AccountActivationToken;
import com.mrrg.backend.model.User;
import com.mrrg.backend.model.UserRole;
import com.mrrg.backend.model.UserStatus;
import com.mrrg.backend.repository.AccountActivationTokenRepository;
import com.mrrg.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountActivationTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private JobService jobService;

    @InjectMocks
    private UserManagementService userManagementService;

    @Test
    void listAllUsers_shouldReturnAllUsers() {
        User user1 = new User("user1@test.com", "password", "User 1", UserRole.EMPLOYEE);
        user1.setId(1L);
        user1.setEnabled(true);

        when(userRepository.findAll()).thenReturn(List.of(user1));
        lenient().when(tokenRepository.existsByUser_IdAndUsedAtIsNullAndExpiresAtGreaterThan(anyLong(), anyLong())).thenReturn(false);

        List<UserManagementResponse> result = userManagementService.listAllUsers();

        assertThat(result).hasSize(1);
        verify(userRepository).findAll();
    }

    @Test
    void getUserById_shouldReturnUser() {
        User user = new User("user@test.com", "password", "Test User", UserRole.EMPLOYEE);
        user.setId(1L);
        user.setEnabled(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        lenient().when(tokenRepository.existsByUser_IdAndUsedAtIsNullAndExpiresAtGreaterThan(eq(1L), anyLong())).thenReturn(false);

        UserManagementResponse result = userManagementService.getUserById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void getUserById_shouldThrowNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userManagementService.getUserById(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);
    }

    @Test
    void validateAdminInvitationPermission_shouldThrowWhenNotAdmin() {
        User nonAdmin = new User("manager@test.com", "password", "Manager", UserRole.MANAGER);
        nonAdmin.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(nonAdmin));

        assertThatThrownBy(() -> userManagementService.validateAdminInvitationPermission(1L, UserRole.EMPLOYEE))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }

    @Test
    void validateAdminInvitationPermission_shouldThrowWhenInvitingAdmin() {
        User admin = new User("admin@test.com", "password", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userManagementService.validateAdminInvitationPermission(1L, UserRole.ADMIN))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.FORBIDDEN);
    }

    @Test
    void validateAdminInvitationPermission_shouldSucceedForAdmin() {
        User admin = new User("admin@test.com", "password", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatNoException().isThrownBy(() ->
                userManagementService.validateAdminInvitationPermission(1L, UserRole.EMPLOYEE));
    }

    @Test
    void updateUser_shouldUpdateFields() {
        User admin = new User("admin@test.com", "password", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        User user = new User("user@test.com", "password", "Old", UserRole.EMPLOYEE);
        user.setId(2L);
        user.setEnabled(true);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("New");
        request.setEmail("new@test.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        lenient().when(tokenRepository.existsByUser_IdAndUsedAtIsNullAndExpiresAtGreaterThan(eq(2L), anyLong())).thenReturn(false);

        UserManagementResponse result = userManagementService.updateUser(2L, request, 1L);

        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getEmail()).isEqualTo("new@test.com");
    }

    @Test
    void updateUser_shouldThrowOnDuplicateEmail() {
        User admin = new User("admin@test.com", "password", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        User user = new User("user@test.com", "password", "User", UserRole.EMPLOYEE);
        user.setId(2L);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("existing@test.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("existing@test.com")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userManagementService.updateUser(2L, request, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateUser_shouldThrowOnNullFields() {
        User admin = new User("admin@test.com", "password", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        User user = new User("user@test.com", "password", "User", UserRole.EMPLOYEE);
        user.setId(2L);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName(null);
        request.setEmail(null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userManagementService.updateUser(2L, request, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
    }

    @Test
    void deactivateUser_shouldSetEnabledFalse() {
        User admin = new User("admin@test.com", "password", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        User activeUser = new User("user@test.com", "password", "User", UserRole.EMPLOYEE);
        activeUser.setId(2L);
        activeUser.setEnabled(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(activeUser));
        when(tokenRepository.existsByUser_IdAndUsedAtIsNullAndExpiresAtGreaterThan(eq(2L), anyLong())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        userManagementService.deactivateUser(2L, 1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEnabled()).isFalse();
        verify(jobService).removeUserFromNonFinalJobs(2L);
    }

    @Test
    void deactivateUser_shouldInvalidateTokensForPending() {
        User admin = new User("admin@test.com", "password", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        User pendingUser = new User("user@test.com", "", "User", UserRole.EMPLOYEE);
        pendingUser.setId(2L);
        pendingUser.setEnabled(false);

        AccountActivationToken token = new AccountActivationToken("token", pendingUser, System.currentTimeMillis() + 100000);
        token.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(pendingUser));
        when(tokenRepository.existsByUser_IdAndUsedAtIsNullAndExpiresAtGreaterThan(eq(2L), anyLong())).thenReturn(true);
        when(tokenRepository.findByUser_IdAndUsedAtIsNull(2L)).thenReturn(List.of(token));
        when(userRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        userManagementService.deactivateUser(2L, 1L);

        verify(tokenRepository).saveAll(any());
    }

    @Test
    void deactivateUser_shouldThrowWhenSelfDeactivate() {
        User admin = new User("admin@test.com", "password", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userManagementService.deactivateUser(1L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
    }

    @Test
    void reactivateUser_shouldRestoreActiveUser() {
        User admin = new User("admin@test.com", "password", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        User disabledUser = new User("user@test.com", "hashed", "User", UserRole.EMPLOYEE);
        disabledUser.setId(2L);
        disabledUser.setEnabled(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(disabledUser));
        when(tokenRepository.existsByUser_IdAndUsedAtIsNullAndExpiresAtGreaterThan(eq(2L), anyLong())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        userManagementService.reactivateUser(2L, 1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEnabled()).isTrue();
    }

    @Test
    void reactivateUser_shouldThrowForPendingUser() {
        User admin = new User("admin@test.com", "password", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        User pendingUser = new User("user@test.com", "", "User", UserRole.EMPLOYEE);
        pendingUser.setId(2L);
        pendingUser.setEnabled(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(pendingUser));
        when(tokenRepository.existsByUser_IdAndUsedAtIsNullAndExpiresAtGreaterThan(eq(2L), anyLong())).thenReturn(true);

        assertThatThrownBy(() -> userManagementService.reactivateUser(2L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
    }

    @Test
    void reactivateUser_shouldRestorePendingActivationForNeverActivatedUser() {
        User admin = new User("admin@test.com", "password", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        User neverActivated = new User("user@test.com", "", "User", UserRole.EMPLOYEE);
        neverActivated.setId(2L);
        neverActivated.setEnabled(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(neverActivated));
        when(tokenRepository.existsByUser_IdAndUsedAtIsNullAndExpiresAtGreaterThan(eq(2L), anyLong())).thenReturn(false);
        when(tokenRepository.findByUser_IdAndUsedAtIsNull(2L)).thenReturn(new ArrayList<>());
        when(tokenRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(userRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        userManagementService.reactivateUser(2L, 1L);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEnabled()).isFalse();

        verify(tokenRepository).save(any(AccountActivationToken.class));
        verify(emailService).sendActivationEmail(eq("user@test.com"), anyString(), eq("User"));
    }

    @Test
    void reactivateUser_shouldThrowForAlreadyActiveUser() {
        User admin = new User("admin@test.com", "password", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        User activeUser = new User("user@test.com", "password", "User", UserRole.EMPLOYEE);
        activeUser.setId(2L);
        activeUser.setEnabled(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(activeUser));
        lenient().when(tokenRepository.existsByUser_IdAndUsedAtIsNullAndExpiresAtGreaterThan(eq(2L), anyLong())).thenReturn(false);

        assertThatThrownBy(() -> userManagementService.reactivateUser(2L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
    }

    @Test
    void resendActivationLink_shouldCreateNewToken() {
        User admin = new User("admin@test.com", "password", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        User pendingUser = new User("user@test.com", "", "User", UserRole.EMPLOYEE);
        pendingUser.setId(2L);
        pendingUser.setEnabled(false);

        AccountActivationToken oldToken = new AccountActivationToken("old", pendingUser, System.currentTimeMillis() + 100000);
        oldToken.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(pendingUser));
        when(tokenRepository.findByUser_IdAndUsedAtIsNull(2L)).thenReturn(List.of(oldToken));
        when(tokenRepository.existsByUser_IdAndUsedAtIsNullAndExpiresAtGreaterThan(eq(2L), anyLong())).thenReturn(true);
        when(tokenRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        userManagementService.resendActivationLink(2L, 1L);

        verify(tokenRepository).saveAll(any());
        verify(tokenRepository).save(any(AccountActivationToken.class));
        verify(emailService).sendActivationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void resendActivationLink_shouldThrowForActiveUser() {
        User admin = new User("admin@test.com", "password", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        User activeUser = new User("user@test.com", "password", "User", UserRole.EMPLOYEE);
        activeUser.setId(2L);
        activeUser.setEnabled(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(activeUser));
        lenient().when(tokenRepository.existsByUser_IdAndUsedAtIsNullAndExpiresAtGreaterThan(eq(2L), anyLong())).thenReturn(false);

        assertThatThrownBy(() -> userManagementService.resendActivationLink(2L, 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
    }

    @Test
    void resendActivationLink_shouldWorkForNeverActivatedDisabled() {
        User admin = new User("admin@test.com", "password", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        User neverActivated = new User("user@test.com", "", "User", UserRole.EMPLOYEE);
        neverActivated.setId(2L);
        neverActivated.setEnabled(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(neverActivated));
        when(tokenRepository.findByUser_IdAndUsedAtIsNull(2L)).thenReturn(new ArrayList<>());
        when(tokenRepository.existsByUser_IdAndUsedAtIsNullAndExpiresAtGreaterThan(eq(2L), anyLong())).thenReturn(false);
        when(tokenRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        userManagementService.resendActivationLink(2L, 1L);

        verify(tokenRepository).save(any(AccountActivationToken.class));
        verify(emailService).sendActivationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void computeStatus_shouldReturnActive() {
        User user = new User("user@test.com", "password", "User", UserRole.EMPLOYEE);
        user.setId(1L);
        user.setEnabled(true);

        lenient().when(tokenRepository.existsByUser_IdAndUsedAtIsNullAndExpiresAtGreaterThan(eq(1L), anyLong())).thenReturn(false);

        UserStatus status = userManagementService.computeStatus(user);

        assertThat(status).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void computeStatus_shouldReturnPending() {
        User user = new User("user@test.com", "", "User", UserRole.EMPLOYEE);
        user.setId(1L);
        user.setEnabled(false);

        when(tokenRepository.existsByUser_IdAndUsedAtIsNullAndExpiresAtGreaterThan(eq(1L), anyLong())).thenReturn(true);

        UserStatus status = userManagementService.computeStatus(user);

        assertThat(status).isEqualTo(UserStatus.PENDING_ACTIVATION);
    }

    @Test
    void computeStatus_shouldReturnDisabled() {
        User user = new User("user@test.com", "password", "User", UserRole.EMPLOYEE);
        user.setId(1L);
        user.setEnabled(false);

        when(tokenRepository.existsByUser_IdAndUsedAtIsNullAndExpiresAtGreaterThan(eq(1L), anyLong())).thenReturn(false);

        UserStatus status = userManagementService.computeStatus(user);

        assertThat(status).isEqualTo(UserStatus.DISABLED);
    }

    @Test
    void updateUser_shouldNormalizeEmailToLowercase() {
        User admin = new User("admin@test.com", "password", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        User userToUpdate = new User("old@test.com", "password", "Test User", UserRole.EMPLOYEE);
        userToUpdate.setId(2L);
        userToUpdate.setEnabled(true);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Updated Name");
        request.setEmail("NEW@TEST.COM");

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(userToUpdate));
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);
        lenient().when(tokenRepository.existsByUser_IdAndUsedAtIsNullAndExpiresAtGreaterThan(eq(2L), anyLong())).thenReturn(false);
        lenient().doNothing().when(emailService).sendEmailChangeNotification(anyString(), anyString(), anyString());

        userManagementService.updateUser(2L, request, 1L);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("new@test.com");
    }

    @Test
    void updateUser_shouldThrowBadRequest_withClearMessage_whenDuplicateEmailExists() {
        User admin = new User("admin@test.com", "password", "Admin", UserRole.ADMIN);
        admin.setId(1L);

        User userToUpdate = new User("old@test.com", "password", "Test User", UserRole.EMPLOYEE);
        userToUpdate.setId(2L);
        userToUpdate.setEnabled(true);

        User existingUser = new User("new@test.com", "password", "Existing User", UserRole.EMPLOYEE);
        existingUser.setId(3L);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setName("Updated Name");
        request.setEmail("NEW@TEST.COM");

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(userToUpdate));
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> 
            userManagementService.updateUser(2L, request, 1L)
        )
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("already exists with this email");
    }

    @Test
    void getAssignableWorkers_shouldReturnOnlyActiveEmployeesAndManagers() {
        User activeEmployee = new User("active@test.com", "password", "Active", UserRole.EMPLOYEE);
        activeEmployee.setId(1L);
        activeEmployee.setEnabled(true);

        User disabledEmployee = new User("disabled@test.com", "password", "Disabled", UserRole.EMPLOYEE);
        disabledEmployee.setId(2L);
        disabledEmployee.setEnabled(false);

        User pendingEmployee = new User("pending@test.com", "", "Pending", UserRole.EMPLOYEE);
        pendingEmployee.setId(3L);
        pendingEmployee.setEnabled(false);

        when(userRepository.findByRoleInOrderByNameAsc(anyList()))
                .thenReturn(List.of(activeEmployee, disabledEmployee, pendingEmployee));
        when(tokenRepository.existsByUser_IdAndUsedAtIsNullAndExpiresAtGreaterThan(eq(2L), anyLong())).thenReturn(false);
        when(tokenRepository.existsByUser_IdAndUsedAtIsNullAndExpiresAtGreaterThan(eq(3L), anyLong())).thenReturn(true);
        lenient().when(tokenRepository.existsByUser_IdAndUsedAtIsNullAndExpiresAtGreaterThan(eq(1L), anyLong())).thenReturn(false);

        List<UserSummary> result = userManagementService.getAssignableWorkers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getName()).isEqualTo("Active");
    }
}
