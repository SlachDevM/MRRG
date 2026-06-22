package com.mrrg.backend.service;

import com.mrrg.backend.model.Job;
import com.mrrg.backend.model.Notification;
import com.mrrg.backend.model.NotificationType;
import com.mrrg.backend.model.User;
import com.mrrg.backend.model.UserRole;
import com.mrrg.backend.repository.JobRepository;
import com.mrrg.backend.repository.NotificationRepository;
import com.mrrg.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
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
    private JobRepository jobRepository;

    @Mock
    private FirebaseNotificationService firebaseNotificationService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void create_shouldPersistNotificationAndSendFcm() {
        User user = userWithId(1L);
        user.setFcmToken("valid-fcm-token");
        Job job = jobWithId(100L);

        Notification notification = new Notification(user, job, NotificationType.JOB_ASSIGNED, "You have a new job");
        notification.setId(1L);

        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(jobRepository.getReferenceById(100L)).thenReturn(job);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Notification result = notificationService.create(1L, 100L, NotificationType.JOB_ASSIGNED, "You have a new job");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getJobId()).isEqualTo(100L);
        assertThat(result.getMessage()).isEqualTo("You have a new job");

        verify(notificationRepository).save(any(Notification.class));
        verify(userRepository).getReferenceById(1L);
        verify(jobRepository).getReferenceById(100L);
        verify(userRepository).findById(1L);
        verify(firebaseNotificationService).sendToUser(eq(user), anyString(), anyString(), anyMap());
    }

    @Test
    void create_shouldPersistNotificationEvenIfFcmFails() {
        User user = userWithId(1L);
        user.setFcmToken("valid-fcm-token");
        Job job = jobWithId(100L);

        Notification notification = new Notification(user, job, NotificationType.JOB_ASSIGNED, "You have a new job");
        notification.setId(1L);

        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(jobRepository.getReferenceById(100L)).thenReturn(job);
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
        User userReference = userWithId(1L);
        Job job = jobWithId(100L);
        Notification notification = new Notification(userReference, job, NotificationType.JOB_ASSIGNED, "You have a new job");
        notification.setId(1L);

        when(userRepository.getReferenceById(1L)).thenReturn(userReference);
        when(jobRepository.getReferenceById(100L)).thenReturn(job);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        Notification result = notificationService.create(1L, 100L, NotificationType.JOB_ASSIGNED, "You have a new job");

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getJobId()).isEqualTo(100L);

        verify(notificationRepository).save(any(Notification.class));
        verify(firebaseNotificationService, never()).sendToUser(any(User.class), anyString(), anyString(), anyMap());
    }

    @Test
    void create_shouldIncludeJobIdInDataPayload() {
        User user = userWithId(1L);
        user.setFcmToken("valid-fcm-token");
        Job job = jobWithId(100L);

        Notification notification = new Notification(user, job, NotificationType.JOB_READY_FOR_CONFIRMATION, "Job ready for confirmation");
        notification.setId(2L);

        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(jobRepository.getReferenceById(100L)).thenReturn(job);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        notificationService.create(1L, 100L, NotificationType.JOB_READY_FOR_CONFIRMATION, "Job ready for confirmation");

        ArgumentCaptor<Map<String, String>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(firebaseNotificationService).sendToUser(any(User.class), anyString(), anyString(), dataCaptor.capture());

        assertThat(dataCaptor.getValue().get("jobId")).isEqualTo("100");
    }

    @Test
    void create_shouldGenerateProperTitleForEachNotificationType() {
        User user = userWithId(1L);
        user.setFcmToken("valid-fcm-token");
        Job job = jobWithId(100L);

        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(jobRepository.getReferenceById(100L)).thenReturn(job);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        for (NotificationType type : NotificationType.values()) {
            Notification notification = new Notification(user, job, type, "Message for " + type);
            notification.setId((long) type.ordinal());

            when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

            notificationService.create(1L, 100L, type, "Message for " + type);

            ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
            verify(firebaseNotificationService, atLeastOnce()).sendToUser(eq(user), titleCaptor.capture(), anyString(), anyMap());

            assertThat(titleCaptor.getValue()).isNotBlank();
        }
    }

    private User userWithId(long id) {
        User user = new User("user@test.com", "password", "Test User", UserRole.EMPLOYEE);
        user.setId(id);
        return user;
    }

    private Job jobWithId(long id) {
        Job job = new Job();
        job.setId(id);
        return job;
    }
}
