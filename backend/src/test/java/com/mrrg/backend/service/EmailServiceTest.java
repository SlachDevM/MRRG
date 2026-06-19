package com.mrrg.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Test
    void isDevelopment_returnsTrue_forDevProfile() {
        EmailService emailService = new EmailService();
        ReflectionTestUtils.setField(emailService, "activeProfile", "dev");

        boolean isDev = (boolean) ReflectionTestUtils.invokeMethod(emailService, "isDevelopment");
        assertThat(isDev).isTrue();
    }

    @Test
    void isDevelopment_returnsTrue_forDevelopmentProfile() {
        EmailService emailService = new EmailService();
        ReflectionTestUtils.setField(emailService, "activeProfile", "development");

        boolean isDev = (boolean) ReflectionTestUtils.invokeMethod(emailService, "isDevelopment");
        assertThat(isDev).isTrue();
    }

    @Test
    void isDevelopment_returnsTrue_forLocalProfile() {
        EmailService emailService = new EmailService();
        ReflectionTestUtils.setField(emailService, "activeProfile", "local");

        boolean isDev = (boolean) ReflectionTestUtils.invokeMethod(emailService, "isDevelopment");
        assertThat(isDev).isTrue();
    }

    @Test
    void isDevelopment_returnsFalse_forProductionProfile() {
        EmailService emailService = new EmailService();
        ReflectionTestUtils.setField(emailService, "activeProfile", "prod");

        boolean isDev = (boolean) ReflectionTestUtils.invokeMethod(emailService, "isDevelopment");
        assertThat(isDev).isFalse();
    }

    @Test
    void isDevelopment_returnsFalse_forProductionProfileExplicit() {
        EmailService emailService = new EmailService();
        ReflectionTestUtils.setField(emailService, "activeProfile", "production");

        boolean isDev = (boolean) ReflectionTestUtils.invokeMethod(emailService, "isDevelopment");
        assertThat(isDev).isFalse();
    }

    @Test
    void sendActivationEmail_logsInDevelopmentProfile() {
        EmailService emailService = new EmailService();
        ReflectionTestUtils.setField(emailService, "activeProfile", "dev");
        ReflectionTestUtils.setField(emailService, "activationLinkBaseUrl", "mrrg://activate-account");

        // Ensure no exception is thrown during development logging
        assertThatNoException().isThrownBy(() ->
            emailService.sendActivationEmail("user@test.com", "secret-token-123", "Test User")
        );
    }

    @Test
    void sendActivationEmail_skipsLoggingInProductionProfile() {
        EmailService emailService = new EmailService();
        ReflectionTestUtils.setField(emailService, "activeProfile", "prod");
        ReflectionTestUtils.setField(emailService, "activationLinkBaseUrl", "mrrg://activate-account");

        // Ensure no exception is thrown (falls through to sendEmailViaSMTP which logs warning)
        assertThatNoException().isThrownBy(() ->
            emailService.sendActivationEmail("user@test.com", "secret-token-123", "Test User")
        );
    }

    @Test
    void buildActivationLink_formatsDeepLink() {
        EmailService emailService = new EmailService();
        ReflectionTestUtils.setField(emailService, "activationLinkBaseUrl", "mrrg://activate-account");

        String link = (String) ReflectionTestUtils.invokeMethod(emailService, "buildActivationLink", "test-token-123");
        assertThat(link).isEqualTo("mrrg://activate-account?token=test-token-123");
    }

    @Test
    void buildActivationLink_formatsHttpsLink() {
        EmailService emailService = new EmailService();
        ReflectionTestUtils.setField(emailService, "activationLinkBaseUrl", "https://example.com/activate");

        String link = (String) ReflectionTestUtils.invokeMethod(emailService, "buildActivationLink", "test-token-456");
        assertThat(link).isEqualTo("https://example.com/activate?token=test-token-456");
    }
}
