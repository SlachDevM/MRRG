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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

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
    void getUserNotifications_shouldReturnNotificationsForUser() {
        User user = userWithId(1L);
        Notification notification = new Notification(user, jobWithId(100L), NotificationType.JOB_ASSIGNED, "Assigned");

        when(notificationRepository.findByUser_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(notification));

        List<Notification> result = notificationService.getUserNotifications(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
        assertThat(result.get(0).getJobId()).isEqualTo(100L);
    }

    @Test
    void getUnreadCount_shouldReturnRepositoryCount() {
        when(notificationRepository.countByUser_IdAndIsReadFalse(1L)).thenReturn(3L);

        long count = notificationService.getUnreadCount(1L);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    void markAsRead_shouldSetNotificationAsRead() {
        Notification notification = new Notification(
                userWithId(1L),
                jobWithId(100L),
                NotificationType.JOB_ASSIGNED,
                "Job assigned"
        );
        notification.setId(5L);
        notification.setIsRead(false);

        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        Notification result = notificationService.markAsRead(5L);

        assertThat(result.getIsRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_shouldThrowNotFound_whenNotificationDoesNotExist() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void create_shouldCreateUnreadNotificationWithUserAndJobReferences() {
        User user = userWithId(1L);
        Job job = jobWithId(100L);

        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(jobRepository.getReferenceById(100L)).thenReturn(job);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationService.create(
                1L,
                100L,
                NotificationType.JOB_ASSIGNED,
                "You have been assigned"
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();

        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getJob()).isSameAs(job);
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getJobId()).isEqualTo(100L);
        assertThat(saved.getType()).isEqualTo(NotificationType.JOB_ASSIGNED);
        assertThat(saved.getMessage()).isEqualTo("You have been assigned");
        assertThat(saved.getIsRead()).isFalse();

        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getJobId()).isEqualTo(100L);
        assertThat(result.getIsRead()).isFalse();
    }

    @Test
    void create_shouldAllowNotificationWithoutJob() {
        User user = userWithId(1L);

        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationService.create(
                1L,
                null,
                NotificationType.JOB_ASSIGNED,
                "General message"
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        verify(jobRepository, never()).getReferenceById(anyLong());

        assertThat(captor.getValue().getJob()).isNull();
        assertThat(captor.getValue().getJobId()).isNull();
        assertThat(result.getJobId()).isNull();
    }

    @Test
    void markAllAsRead_shouldMarkEveryUnreadNotificationAsRead() {
        User user = userWithId(1L);
        Notification first = new Notification(user, jobWithId(100L), NotificationType.JOB_ASSIGNED, "First");
        Notification second = new Notification(user, jobWithId(101L), NotificationType.JOB_RESCHEDULED, "Second");

        when(notificationRepository.findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(first, second));

        notificationService.markAllAsRead(1L);

        assertThat(first.getIsRead()).isTrue();
        assertThat(second.getIsRead()).isTrue();

        verify(notificationRepository).saveAll(List.of(first, second));
    }

    private User userWithId(long id) {
        User user = new User("user@test.com", "password", "User", UserRole.EMPLOYEE);
        user.setId(id);
        return user;
    }

    private Job jobWithId(long id) {
        Job job = new Job();
        job.setId(id);
        return job;
    }
}
