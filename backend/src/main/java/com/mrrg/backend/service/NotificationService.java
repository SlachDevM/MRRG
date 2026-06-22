package com.mrrg.backend.service;

import com.mrrg.backend.model.Notification;
import com.mrrg.backend.model.NotificationType;
import com.mrrg.backend.model.User;
import com.mrrg.backend.repository.NotificationRepository;
import com.mrrg.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final FirebaseNotificationService firebaseNotificationService;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            FirebaseNotificationService firebaseNotificationService
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.firebaseNotificationService = firebaseNotificationService;
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository
                .findByUser_IdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository
                .findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository
                .countByUser_IdAndIsReadFalse(userId);
    }

    public Notification markAsRead(Long notificationId) {
        Notification notification =
                notificationRepository.findById(notificationId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        notification.setIsRead(true);

        return notificationRepository.save(notification);
    }

    public void markAllAsRead(Long userId) {
        List<Notification> notifications =
                getUnreadNotifications(userId);

        notifications.forEach(n -> n.setIsRead(true));

        notificationRepository.saveAll(notifications);
    }

    public Notification create(
            Long userId,
            Long jobId,
            NotificationType type,
            String message
    ) {
        User user = userRepository.getReferenceById(userId);
        Notification notification = new Notification(user, jobId, type, message);

        Notification savedNotification = notificationRepository.save(notification);

        try {
            User userForFcm = userRepository.findById(userId).orElse(null);
            if (userForFcm != null) {
                Map<String, String> data = new HashMap<>();
                data.put("notificationId", String.valueOf(savedNotification.getId()));
                data.put("notificationType", type.toString());
                if (jobId != null) {
                    data.put("jobId", String.valueOf(jobId));
                }

                String title = generateTitle(type);
                firebaseNotificationService.sendToUser(userForFcm, title, message, data);
            }
        } catch (Exception e) {
            // Log error but do not throw - notification must be persisted even if FCM fails
        }

        return savedNotification;
    }

    private String generateTitle(NotificationType type) {
        return switch (type) {
            case JOB_ASSIGNED -> "Job Assigned";
            case JOB_RESCHEDULED -> "Job Rescheduled";
            case JOB_READY_FOR_CONFIRMATION -> "Job Ready for Confirmation";
            case JOB_CONFIRMED -> "Job Confirmed";
        };
    }
}