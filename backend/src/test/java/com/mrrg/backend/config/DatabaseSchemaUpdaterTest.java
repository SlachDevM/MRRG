package com.mrrg.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseSchemaUpdaterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DatabaseSchemaUpdater schemaUpdater;

    @Test
    void onApplicationEvent_addsEnabledColumnIfNotExists() {
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);

        schemaUpdater.onApplicationEvent(event);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).execute(sqlCaptor.capture());

        String addEnabledColumn = sqlCaptor.getAllValues().stream()
                .filter(sql -> sql.contains("ADD COLUMN IF NOT EXISTS enabled BOOLEAN DEFAULT TRUE"))
                .findFirst()
                .orElse(null);

        assertThat(addEnabledColumn)
                .as("Should add enabled column with default value TRUE for backward compatibility")
                .isNotNull();
    }

    @Test
    void onApplicationEvent_ensuresExistingUsersRemainEnabled() {
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);

        schemaUpdater.onApplicationEvent(event);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).execute(sqlCaptor.capture());

        String updateExistingUsers = sqlCaptor.getAllValues().stream()
                .filter(sql -> sql.contains("UPDATE users SET enabled = TRUE"))
                .findFirst()
                .orElse(null);

        assertThat(updateExistingUsers)
                .as("Should update existing users to enabled=TRUE to preserve backward compatibility")
                .isNotNull();
    }

    @Test
    void onApplicationEvent_addsFcmTokenColumn() {
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);

        schemaUpdater.onApplicationEvent(event);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).execute(sqlCaptor.capture());

        String addFcmColumn = sqlCaptor.getAllValues().stream()
                .filter(sql -> sql.contains("ADD COLUMN IF NOT EXISTS fcm_token TEXT"))
                .findFirst()
                .orElse(null);

        assertThat(addFcmColumn)
                .as("Should add fcm_token column for Firebase Cloud Messaging")
                .isNotNull();
    }

    @Test
    void onApplicationEvent_addsPhotoColumns() {
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);

        schemaUpdater.onApplicationEvent(event);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).execute(sqlCaptor.capture());

        String beforePhotos = sqlCaptor.getAllValues().stream()
                .filter(sql -> sql.contains("before_photos"))
                .findFirst()
                .orElse(null);

        String afterPhotos = sqlCaptor.getAllValues().stream()
                .filter(sql -> sql.contains("after_photos"))
                .findFirst()
                .orElse(null);

        assertThat(beforePhotos).as("Should add before_photos column").isNotNull();
        assertThat(afterPhotos).as("Should add after_photos column").isNotNull();
    }

    @Test
    void onApplicationEvent_updateInProgressStatusConstraint() {
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);

        schemaUpdater.onApplicationEvent(event);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).execute(sqlCaptor.capture());

        String statusConstraint = sqlCaptor.getAllValues().stream()
                .filter(sql -> sql.contains("IN_PROGRESS"))
                .findFirst()
                .orElse(null);

        assertThat(statusConstraint)
                .as("Should include IN_PROGRESS in jobs status constraint")
                .isNotNull();
    }
}
