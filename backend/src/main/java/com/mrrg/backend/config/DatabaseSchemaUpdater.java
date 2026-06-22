package com.mrrg.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(
        name = "app.database-schema-updater.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class DatabaseSchemaUpdater implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaUpdater.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaUpdater(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        jdbcTemplate.execute("ALTER TABLE jobs DROP CONSTRAINT IF EXISTS jobs_status_check");
        jdbcTemplate.execute(
                "ALTER TABLE jobs ADD CONSTRAINT jobs_status_check CHECK (status IN ("
                        + "'PENDING', 'SCHEDULED', 'IN_PROGRESS', 'READY_FOR_CONFIRMATION', 'DONE', 'TO_BE_FIXED', 'ARCHIVED'"
                        + "))"
        );

        jdbcTemplate.execute("ALTER TABLE jobs ADD COLUMN IF NOT EXISTS before_photos TEXT");
        jdbcTemplate.execute("ALTER TABLE jobs ADD COLUMN IF NOT EXISTS after_photos TEXT");
        
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS fcm_token TEXT");
        
        // Ensure existing users remain enabled (backward compatibility)
        // The DEFAULT TRUE handles any NEW users created by JPA without explicit enabled value
        jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS enabled BOOLEAN DEFAULT TRUE");
        // CRITICAL: Only migrate NULL values (existing users from before this feature)
        // Do NOT reactivate users that were explicitly disabled (enabled = FALSE)
        // This preserves the intended disabled state of any manually disabled accounts
        jdbcTemplate.execute("UPDATE users SET enabled = TRUE WHERE enabled IS NULL");

        migrateLegacyPhotoColumn("before_photo", "before_photos");
        migrateLegacyPhotoColumn("after_photo", "after_photos");
        migrateLegacyAssignedWorkers();
    }

    private void migrateLegacyAssignedWorkers() {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS job_assignments ("
                        + "id BIGSERIAL PRIMARY KEY, "
                        + "job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE, "
                        + "user_id BIGINT NOT NULL REFERENCES users(id), "
                        + "CONSTRAINT uk_job_assignments_job_user UNIQUE (job_id, user_id)"
                        + ")"
        );

        List<Long> jobIds = jdbcTemplate.queryForList(
                "SELECT id FROM jobs "
                        + "WHERE assigned_workers IS NOT NULL "
                        + "AND TRIM(assigned_workers) <> ''",
                Long.class
        );

        for (Long jobId : jobIds) {
            Integer existing = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM job_assignments WHERE job_id = ?",
                    Integer.class,
                    jobId
            );
            if (existing != null && existing > 0) {
                continue;
            }

            String assignedWorkers = jdbcTemplate.queryForObject(
                    "SELECT assigned_workers FROM jobs WHERE id = ?",
                    String.class,
                    jobId
            );
            if (assignedWorkers == null || assignedWorkers.isBlank()) {
                continue;
            }

            for (String part : assignedWorkers.split(",")) {
                String value = part.trim();
                if (value.isEmpty()) {
                    continue;
                }
                try {
                    Long userId = Long.parseLong(value);
                    jdbcTemplate.update(
                            "INSERT INTO job_assignments (job_id, user_id) VALUES (?, ?) "
                                    + "ON CONFLICT (job_id, user_id) DO NOTHING",
                            jobId,
                            userId
                    );
                } catch (NumberFormatException e) {
                    log.warn(
                            "Skipping legacy assigned worker value for job {} (not a user ID): {}",
                            jobId,
                            value
                    );
                }
            }
        }
    }

    private void migrateLegacyPhotoColumn(String legacyColumn, String newColumn) {
        try {
            String dataType = jdbcTemplate.queryForObject(
                    "SELECT udt_name FROM information_schema.columns "
                            + "WHERE table_schema = current_schema() "
                            + "AND table_name = 'jobs' AND column_name = ?",
                    String.class,
                    legacyColumn
            );

            if (dataType == null) {
                return;
            }

            String encodedExpression = switch (dataType) {
                case "bytea" -> "encode(" + legacyColumn + ", 'base64')";
                case "oid" -> "encode(lo_get(" + legacyColumn + "), 'base64')";
                default -> {
                    log.warn(
                            "Skipping legacy photo migration for {} (unsupported type: {})",
                            legacyColumn,
                            dataType
                    );
                    yield null;
                }
            };

            if (encodedExpression == null) {
                return;
            }

            jdbcTemplate.update(
                    "UPDATE jobs SET " + newColumn + " = json_build_array(" + encodedExpression + ")::text "
                            + "WHERE " + legacyColumn + " IS NOT NULL "
                            + "AND (" + newColumn + " IS NULL OR " + newColumn + " = '' OR " + newColumn + " = '[]')"
            );
        } catch (Exception e) {
            log.warn("Legacy photo migration skipped for {}: {}", legacyColumn, e.getMessage());
        }
    }
}
