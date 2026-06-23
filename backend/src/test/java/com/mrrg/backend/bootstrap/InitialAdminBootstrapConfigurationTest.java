package com.mrrg.backend.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class InitialAdminBootstrapConfigurationTest {

    @Test
    void initialAdminBootstrap_shouldBeEnabledByDefault() {
        ConditionalOnProperty conditional = InitialAdminBootstrap.class
                .getAnnotation(ConditionalOnProperty.class);

        assertThat(conditional).isNotNull();
        assertThat(conditional.name()).containsExactly("app.bootstrap.initial-admin.enabled");
        assertThat(conditional.havingValue()).isEqualTo("true");
        assertThat(conditional.matchIfMissing()).isTrue();
    }

    @Test
    void applicationProperties_shouldProvideLocalDevelopmentDefaults() throws IOException {
        Properties properties = loadProperties("application.properties");

        assertThat(properties.getProperty("app.bootstrap.initial-admin.enabled"))
                .isEqualTo("${APP_BOOTSTRAP_INITIAL_ADMIN_ENABLED:true}");
        assertThat(properties.getProperty("app.bootstrap.initial-admin.email"))
                .isEqualTo("${INITIAL_ADMIN_EMAIL:admin@mrrg.local}");
        assertThat(properties.getProperty("app.bootstrap.initial-admin.password"))
                .isEqualTo("${INITIAL_ADMIN_PASSWORD:test}");
        assertThat(properties.getProperty("app.bootstrap.initial-admin.name"))
                .isEqualTo("${INITIAL_ADMIN_NAME:Administrator}");
    }

    @Test
    void applicationProdProperties_shouldNotHardcodeBootstrapCredentials() throws IOException {
        Properties properties = loadProperties("application-prod.properties");

        assertThat(properties.getProperty("app.bootstrap.initial-admin.enabled"))
                .isEqualTo("${APP_BOOTSTRAP_INITIAL_ADMIN_ENABLED:true}");
        assertThat(properties.getProperty("app.bootstrap.initial-admin.email"))
                .isEqualTo("${INITIAL_ADMIN_EMAIL:}");
        assertThat(properties.getProperty("app.bootstrap.initial-admin.password"))
                .isEqualTo("${INITIAL_ADMIN_PASSWORD:}");
        assertThat(properties.getProperty("app.bootstrap.initial-admin.name"))
                .isEqualTo("${INITIAL_ADMIN_NAME:Administrator}");
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
