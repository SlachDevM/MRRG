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

import java.util.List;

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
    void onApplicationEvent_migratesOnlyNullEnabledToTrue() {
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);

        schemaUpdater.onApplicationEvent(event);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).execute(sqlCaptor.capture());

        String updateExistingUsers = sqlCaptor.getAllValues().stream()
                .filter(sql -> sql.contains("UPDATE users SET enabled = TRUE"))
                .findFirst()
                .orElse(null);

        assertThat(updateExistingUsers)
                .as("Should update only NULL enabled values to preserve intentionally disabled users")
                .isNotNull()
                .contains("WHERE enabled IS NULL")
                .doesNotContain("enabled = FALSE");
    }

    @Test
    void onApplicationEvent_preservesIntentionallyDisabledUsers() {
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);

        schemaUpdater.onApplicationEvent(event);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).execute(sqlCaptor.capture());

        String migrationSql = sqlCaptor.getAllValues().stream()
                .filter(sql -> sql.contains("UPDATE users SET enabled = TRUE"))
                .findFirst()
                .orElse(null);

        assertThat(migrationSql)
                .as("Migration should NOT include 'enabled = FALSE' to avoid reactivating explicitly disabled users")
                .doesNotContain("enabled = FALSE");
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
    void onApplicationEvent_migratesLegacyAssignedWorkers() {
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);

        when(jdbcTemplate.queryForList(
                "SELECT id FROM jobs "
                        + "WHERE assigned_workers IS NOT NULL "
                        + "AND TRIM(assigned_workers) <> ''",
                Long.class
        )).thenReturn(List.of(10L));
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM job_assignments WHERE job_id = ?"),
                eq(Integer.class),
                eq(10L)
        )).thenReturn(0);
        when(jdbcTemplate.queryForObject(
                eq("SELECT assigned_workers FROM jobs WHERE id = ?"),
                eq(String.class),
                eq(10L)
        )).thenReturn("2,3");

        schemaUpdater.onApplicationEvent(event);

        verify(jdbcTemplate).update(
                eq("INSERT INTO job_assignments (job_id, user_id) VALUES (?, ?) "
                        + "ON CONFLICT (job_id, user_id) DO NOTHING"),
                eq(10L),
                eq(2L)
        );
        verify(jdbcTemplate).update(
                eq("INSERT INTO job_assignments (job_id, user_id) VALUES (?, ?) "
                        + "ON CONFLICT (job_id, user_id) DO NOTHING"),
                eq(10L),
                eq(3L)
        );
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
