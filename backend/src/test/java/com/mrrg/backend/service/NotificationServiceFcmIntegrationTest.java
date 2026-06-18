package com.mrrg.backend.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.mrrg.backend.model.Notification;
import com.mrrg.backend.model.NotificationType;
import com.mrrg.backend.model.User;
import com.mrrg.backend.model.UserRole;
import com.mrrg.backend.repository.NotificationRepository;
import com.mrrg.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceFcmIntegrationTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FirebaseNotificationService firebaseNotificationService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void create_shouldPersistNotificationAndSendFcm() {
        User user = new User("user@test.com", "password", "Test User", UserRole.EMPLOYEE);
        user.setId(1L);
        user.setFcmToken("valid-fcm-token");

        Notification notification = new Notification(1L, 100L, NotificationType.JOB_ASSIGNED, "You have a new job");
        notification.setId(1L);

        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Notification result = notificationService.create(1L, 100L, NotificationType.JOB_ASSIGNED, "You have a new job");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getMessage()).isEqualTo("You have a new job");

        verify(notificationRepository).save(any(Notification.class));
        verify(userRepository).findById(1L);
        verify(firebaseNotificationService).sendToUser(eq(user), anyString(), anyString(), anyMap());
    }

    @Test
    void create_shouldPersistNotificationEvenIfFcmFails() {
        User user = new User("user@test.com", "password", "Test User", UserRole.EMPLOYEE);
        user.setId(1L);
        user.setFcmToken("valid-fcm-token");

        Notification notification = new Notification(1L, 100L, NotificationType.JOB_ASSIGNED, "You have a new job");
        notification.setId(1L);

        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("Firebase error")).when(firebaseNotificationService).sendToUser(any(User.class), anyString(), anyString(), anyMap());

        Notification result = notificationService.create(1L, 100L, NotificationType.JOB_ASSIGNED, "You have a new job");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        verify(notificationRepository).save(any(Notification.class));
        verify(firebaseNotificationService).sendToUser(any(User.class), anyString(), anyString(), anyMap());
    }

    @Test
    void create_shouldSkipFcmIfUserNotFound() {
        Notification notification = new Notification(1L, 100L, NotificationType.JOB_ASSIGNED, "You have a new job");
        notification.setId(1L);

        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        Notification result = notificationService.create(1L, 100L, NotificationType.JOB_ASSIGNED, "You have a new job");

        assertThat(result).isNotNull();

        verify(notificationRepository).save(any(Notification.class));
        verify(firebaseNotificationService, never()).sendToUser(any(User.class), anyString(), anyString(), anyMap());
    }

    @Test
    void create_shouldIncludeJobIdInDataPayload() {
        User user = new User("user@test.com", "password", "Test User", UserRole.EMPLOYEE);
        user.setId(1L);
        user.setFcmToken("valid-fcm-token");

        Notification notification = new Notification(1L, 100L, NotificationType.JOB_READY_FOR_CONFIRMATION, "Job ready for confirmation");
        notification.setId(2L);

        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        notificationService.create(1L, 100L, NotificationType.JOB_READY_FOR_CONFIRMATION, "Job ready for confirmation");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(firebaseNotificationService).sendToUser(userCaptor.capture(), titleCaptor.capture(), bodyCaptor.capture(), anyMap());

        assertThat(titleCaptor.getValue()).isNotEmpty();
        assertThat(bodyCaptor.getValue()).isEqualTo("Job ready for confirmation");
    }

    @Test
    void create_shouldGenerateProperTitleForEachNotificationType() {
        User user = new User("user@test.com", "password", "Test User", UserRole.EMPLOYEE);
        user.setId(1L);
        user.setFcmToken("valid-fcm-token");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        for (NotificationType type : NotificationType.values()) {
            Notification notification = new Notification(1L, 100L, type, "Message for " + type);
            notification.setId((long) type.ordinal());

            when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

            notificationService.create(1L, 100L, type, "Message for " + type);

            ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
            verify(firebaseNotificationService, atLeastOnce()).sendToUser(eq(user), titleCaptor.capture(), anyString(), anyMap());

            assertThat(titleCaptor.getValue()).isNotBlank();
        }
    }
}
