package com.mrrg.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.*;

class DatabaseSchemaUpdaterConfigurationTest {

    @Test
    void databaseSchemaUpdater_shouldRequireExplicitEnableProperty() {
        ConditionalOnProperty conditional = DatabaseSchemaUpdater.class
                .getAnnotation(ConditionalOnProperty.class);

        assertThat(conditional).isNotNull();
        assertThat(conditional.name()).containsExactly("app.database-schema-updater.enabled");
        assertThat(conditional.havingValue()).isEqualTo("true");
        assertThat(conditional.matchIfMissing()).isFalse();
    }

    @Test
    void applicationProperties_shouldEnableSchemaUpdaterForLocalDev() throws IOException {
        Properties properties = loadProperties("application.properties");

        assertThat(properties.getProperty("app.database-schema-updater.enabled"))
                .isEqualTo("${APP_DATABASE_SCHEMA_UPDATER_ENABLED:true}");
    }

    @Test
    void applicationProdProperties_shouldDisableSchemaUpdater() throws IOException {
        Properties properties = loadProperties("application-prod.properties");

        assertThat(properties.getProperty("app.database-schema-updater.enabled"))
                .isEqualTo("false");
    }

    private Properties loadProperties(String resourceName) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            assertThat(input).as("Expected %s on classpath", resourceName).isNotNull();
            properties.load(input);
        }
        return properties;
    }
}
