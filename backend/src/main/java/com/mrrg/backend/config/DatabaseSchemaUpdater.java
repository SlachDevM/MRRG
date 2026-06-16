package com.mrrg.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
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
                        + "'PENDING', 'SCHEDULED', 'READY_FOR_CONFIRMATION', 'DONE', 'TO_BE_FIXED', 'ARCHIVED'"
                        + "))"
        );

        jdbcTemplate.execute("ALTER TABLE jobs ADD COLUMN IF NOT EXISTS before_photos TEXT");
        jdbcTemplate.execute("ALTER TABLE jobs ADD COLUMN IF NOT EXISTS after_photos TEXT");

        migrateLegacyPhotoColumn("before_photo", "before_photos");
        migrateLegacyPhotoColumn("after_photo", "after_photos");
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
