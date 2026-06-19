package com.mrrg.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Value("${app.activation-link-base-url:mrrg://activate-account}")
    private String activationLinkBaseUrl;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Sends an account activation email to the user.
     * In development, logs the activation link instead of sending email.
     * In production, would use Spring Mail to send the actual email.
     *
     * @param email the recipient email
     * @param token the activation token
     * @param userName the user's name for personalization
     */
    public void sendActivationEmail(String email, String token, String userName) {
        String activationLink = buildActivationLink(token);

        if (isDevelopment()) {
            logActivationLink(email, activationLink, userName);
        } else {
            sendEmailViaSMTP(email, activationLink, userName);
        }
    }

    private String buildActivationLink(String token) {
        if (activationLinkBaseUrl.startsWith("http")) {
            // Web-based link
            return activationLinkBaseUrl + "?token=" + token;
        } else {
            // Deep link format (e.g., mrrg://activate-account?token=...)
            return activationLinkBaseUrl + "?token=" + token;
        }
    }

    private boolean isDevelopment() {
        return "dev".equals(activeProfile) || "development".equals(activeProfile) || "local".equals(activeProfile);
    }

    private void logActivationLink(String email, String link, String userName) {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("ACCOUNT ACTIVATION LINK (Development Mode)");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("To: {}", email);
        log.info("User: {}", userName);
        log.info("Activation Link:");
        log.info("{}", link);
        log.info("═══════════════════════════════════════════════════════════════");
    }

    private void sendEmailViaSMTP(String email, String link, String userName) {
        // TODO: Implement Spring Mail integration for production
        // This would use JavaMailSender or similar to send actual emails
        log.warn("Email sending not yet configured. User: {}, Email: {}", userName, email);
    }
}
