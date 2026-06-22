package com.mrrg.backend.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class NotificationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getJobId_shouldReturnNull_whenJobNotSet() {
        Notification notification = new Notification(
                userWithId(1L),
                null,
                NotificationType.JOB_ASSIGNED,
                "Message"
        );

        assertThat(notification.getJobId()).isNull();
    }

    @Test
    void getJobId_shouldReturnJobId_whenJobReferenceSet() {
        Notification notification = new Notification(
                userWithId(1L),
                jobWithId(100L),
                NotificationType.JOB_ASSIGNED,
                "Message"
        );

        assertThat(notification.getJobId()).isEqualTo(100L);
    }

    @Test
    void jsonSerialization_shouldExposeUserIdAndJobIdWithoutEntities() throws Exception {
        Notification notification = new Notification(
                userWithId(1L),
                jobWithId(100L),
                NotificationType.JOB_ASSIGNED,
                "You have been assigned"
        );
        notification.setId(5L);
        notification.setIsRead(false);
        notification.setCreatedAt(1_700_000_000_000L);

        String json = objectMapper.writeValueAsString(notification);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("userId").asLong()).isEqualTo(1L);
        assertThat(node.get("jobId").asLong()).isEqualTo(100L);
        assertThat(node.get("message").asText()).isEqualTo("You have been assigned");
        assertThat(node.get("type").asText()).isEqualTo("JOB_ASSIGNED");
        assertThat(node.has("user")).isFalse();
        assertThat(node.has("job")).isFalse();
    }

    @Test
    void jsonSerialization_shouldAllowNullJobId() throws Exception {
        Notification notification = new Notification(
                userWithId(1L),
                null,
                NotificationType.JOB_ASSIGNED,
                "Message"
        );
        notification.setId(5L);

        JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(notification));

        assertThat(node.get("userId").asLong()).isEqualTo(1L);
        assertThat(node.get("jobId").isNull()).isTrue();
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
