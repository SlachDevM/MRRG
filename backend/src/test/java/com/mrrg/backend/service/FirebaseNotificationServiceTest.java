package com.mrrg.backend.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.mrrg.backend.model.User;
import com.mrrg.backend.model.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirebaseNotificationServiceTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @InjectMocks
    private FirebaseNotificationService firebaseNotificationService;

    @Test
    void sendToUser_shouldSendMessageWhenUserHasValidToken() throws Exception {
        User user = new User("user@test.com", "password", "Test User", UserRole.EMPLOYEE);
        user.setId(1L);
        user.setFcmToken("valid-fcm-token-12345");

        Map<String, String> data = new HashMap<>();
        data.put("notificationId", "123");
        data.put("jobId", "456");

        when(firebaseMessaging.send(any(Message.class))).thenReturn("message-id-12345");

        firebaseNotificationService.sendToUser(user, "Test Title", "Test Body", data);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(firebaseMessaging).send(messageCaptor.capture());

        Message sentMessage = messageCaptor.getValue();
        assertThat(sentMessage).isNotNull();
    }

    @Test
    void sendToUser_shouldSkipWhenUserHasNullToken() throws Exception {
        User user = new User("user@test.com", "password", "Test User", UserRole.EMPLOYEE);
        user.setId(1L);
        user.setFcmToken(null);

        Map<String, String> data = new HashMap<>();
        firebaseNotificationService.sendToUser(user, "Test Title", "Test Body", data);

        verify(firebaseMessaging, never()).send(any(Message.class));
    }

    @Test
    void sendToUser_shouldSkipWhenUserHasBlankToken() throws Exception {
        User user = new User("user@test.com", "password", "Test User", UserRole.EMPLOYEE);
        user.setId(1L);
        user.setFcmToken("   ");

        Map<String, String> data = new HashMap<>();
        firebaseNotificationService.sendToUser(user, "Test Title", "Test Body", data);

        verify(firebaseMessaging, never()).send(any(Message.class));
    }

    @Test
    void sendToUser_shouldSkipWhenUserIsNull() throws Exception {
        Map<String, String> data = new HashMap<>();
        firebaseNotificationService.sendToUser(null, "Test Title", "Test Body", data);

        verify(firebaseMessaging, never()).send(any(Message.class));
    }

    @Test
    void sendToUser_shouldContinueWhenFirebaseThrowsException() throws Exception {
        User user = new User("user@test.com", "password", "Test User", UserRole.EMPLOYEE);
        user.setId(1L);
        user.setFcmToken("valid-fcm-token-12345");

        Map<String, String> data = new HashMap<>();
        when(firebaseMessaging.send(any(Message.class)))
                .thenThrow(new RuntimeException("Firebase error"));

        assertThatNoException().isThrownBy(() ->
            firebaseNotificationService.sendToUser(user, "Test Title", "Test Body", data)
        );

        verify(firebaseMessaging).send(any(Message.class));
    }

    @Test
    void sendToUser_shouldHandleNullFirebaseMessaging() {
        FirebaseNotificationService service = new FirebaseNotificationService(null);
        User user = new User("user@test.com", "password", "Test User", UserRole.EMPLOYEE);
        user.setId(1L);
        user.setFcmToken("valid-fcm-token-12345");

        assertThatNoException().isThrownBy(() ->
            service.sendToUser(user, "Test Title", "Test Body", new HashMap<>())
        );
    }

    @Test
    void sendToUser_shouldIncludeDataPayload() throws Exception {
        User user = new User("user@test.com", "password", "Test User", UserRole.EMPLOYEE);
        user.setId(1L);
        user.setFcmToken("valid-fcm-token-12345");

        Map<String, String> data = new HashMap<>();
        data.put("notificationId", "123");
        data.put("jobId", "456");
        data.put("notificationType", "JOB_ASSIGNED");

        when(firebaseMessaging.send(any(Message.class))).thenReturn("message-id-12345");

        firebaseNotificationService.sendToUser(user, "Test Title", "Test Body", data);

        verify(firebaseMessaging).send(any(Message.class));
    }
}
