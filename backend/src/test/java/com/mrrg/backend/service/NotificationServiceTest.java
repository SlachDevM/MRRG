package com.mrrg.backend.service;

import com.mrrg.backend.model.Notification;
import com.mrrg.backend.model.NotificationType;
import com.mrrg.backend.repository.NotificationRepository;
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

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void getUnreadCount_shouldReturnRepositoryCount() {
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(3L);

        long count = notificationService.getUnreadCount(1L);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    void markAsRead_shouldSetNotificationAsRead() {
        Notification notification = new Notification(
                1L,
                100L,
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
    void create_shouldCreateUnreadNotification() {
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

        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getJobId()).isEqualTo(100L);
        assertThat(saved.getType()).isEqualTo(NotificationType.JOB_ASSIGNED);
        assertThat(saved.getMessage()).isEqualTo("You have been assigned");
        assertThat(saved.getIsRead()).isFalse();

        assertThat(result.getIsRead()).isFalse();
    }

    @Test
    void markAllAsRead_shouldMarkEveryUnreadNotificationAsRead() {
        Notification first = new Notification(1L, 100L, NotificationType.JOB_ASSIGNED, "First");
        Notification second = new Notification(1L, 101L, NotificationType.JOB_RESCHEDULED, "Second");

        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(first, second));

        notificationService.markAllAsRead(1L);

        assertThat(first.getIsRead()).isTrue();
        assertThat(second.getIsRead()).isTrue();

        verify(notificationRepository).saveAll(List.of(first, second));
    }
}