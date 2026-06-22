package com.mrrg.backend.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AccountActivationTokenTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getUserId_shouldReturnNull_whenUserNotSet() {
        AccountActivationToken token = new AccountActivationToken();

        assertThat(token.getUserId()).isNull();
    }

    @Test
    void getUserId_shouldReturnUserId_whenUserReferenceSet() {
        User user = userWithId(42L);
        AccountActivationToken token = new AccountActivationToken(
                "token-value",
                user,
                System.currentTimeMillis() + 60_000
        );

        assertThat(token.getUserId()).isEqualTo(42L);
        assertThat(token.getUser()).isSameAs(user);
    }

    @Test
    void jsonSerialization_shouldExposeUserIdWithoutUserEntity() throws Exception {
        User user = userWithId(7L);
        AccountActivationToken token = new AccountActivationToken(
                "secure-token",
                user,
                1_700_000_000_000L
        );
        token.setId(3L);

        String json = objectMapper.writeValueAsString(token);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("userId").asLong()).isEqualTo(7L);
        assertThat(node.get("token").asText()).isEqualTo("secure-token");
        assertThat(node.get("expiresAt").asLong()).isEqualTo(1_700_000_000_000L);
        assertThat(node.has("user")).isFalse();
    }

    private User userWithId(long id) {
        User user = new User("user@test.com", "password", "User", UserRole.EMPLOYEE);
        user.setId(id);
        return user;
    }
}
