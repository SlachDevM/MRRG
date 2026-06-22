package com.mrrg.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.activation-link-base-url:mrrg://activate-account}")
    private String activationLinkBaseUrl;

    @Value("${spring.mail.from:noreply@mrrg.local}")
    private String fromEmail;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Sends an account activation email to the user.
     * In development, logs the activation link instead of sending email.
     * In production, sends via Spring Mail.
     *
     * SECURITY: Activation tokens are NEVER logged in production environments.
     * They are only logged in dev/development/local profiles for testing purposes.
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
        return "dev".equalsIgnoreCase(activeProfile) 
                || "development".equalsIgnoreCase(activeProfile) 
                || "local".equalsIgnoreCase(activeProfile);
    }

    /**
     * Logs the activation link. This should ONLY be called in development profiles.
     * SECURITY WARNING: This logs the activation token in plain text.
     * This must never be used in production.
     */
    private void logActivationLink(String email, String link, String userName) {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("ACCOUNT ACTIVATION LINK (Development Mode Only)");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("To: {}", email);
        log.info("User: {}", userName);
        log.info("Activation Link:");
        log.info("{}", link);
        log.info("═══════════════════════════════════════════════════════════════");
    }

    private void sendEmailViaSMTP(String email, String link, String userName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Account Activation - MRRG");
            message.setText(buildActivationEmailBody(userName, link));
            
            mailSender.send(message);
            log.info("Activation email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send activation email to {}: {}", email, e.getMessage(), e);
        }
    }

    private String buildActivationEmailBody(String userName, String link) {
        return String.format(
            "Hello %s,\n\n" +
            "Welcome to MRRG! Your account has been created and is ready to activate.\n\n" +
            "Please click the link below to activate your account:\n" +
            "%s\n\n" +
            "This link will expire in 24 hours.\n\n" +
            "If you did not create this account, please contact an administrator.\n\n" +
            "Best regards,\n" +
            "MRRG Team",
            userName, link
        );
    }

    /**
     * Sends a notification email when a user's email address is changed by an admin.
     * This is a security notification to alert the user of the email change.
     *
     * @param oldEmail the previous email address
     * @param newEmail the new email address
     * @param userName the user's name
     */
    public void sendEmailChangeNotification(String oldEmail, String newEmail, String userName) {
        if (isDevelopment()) {
            logEmailChangeNotification(oldEmail, newEmail, userName);
        } else {
            sendEmailChangeViaSMTP(oldEmail, newEmail, userName);
        }
    }

    private void logEmailChangeNotification(String oldEmail, String newEmail, String userName) {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("EMAIL CHANGE NOTIFICATION (Development Mode Only)");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("User: {}", userName);
        log.info("Old Email: {}", oldEmail);
        log.info("New Email: {}", newEmail);
        log.info("═══════════════════════════════════════════════════════════════");
    }

    private void sendEmailChangeViaSMTP(String oldEmail, String newEmail, String userName) {
        try {
            // Send security notification to old email
            SimpleMailMessage oldEmailMessage = new SimpleMailMessage();
            oldEmailMessage.setFrom(fromEmail);
            oldEmailMessage.setTo(oldEmail);
            oldEmailMessage.setSubject("Email Address Changed - MRRG Account Security Notice");
            oldEmailMessage.setText(buildOldEmailNotificationBody(userName, oldEmail, newEmail));
            
            mailSender.send(oldEmailMessage);
            log.info("Email change security notification sent to old email: {}", oldEmail);
            
            // Send confirmation to new email
            SimpleMailMessage newEmailMessage = new SimpleMailMessage();
            newEmailMessage.setFrom(fromEmail);
            newEmailMessage.setTo(newEmail);
            newEmailMessage.setSubject("Email Address Updated - MRRG Account");
            newEmailMessage.setText(buildNewEmailNotificationBody(userName, oldEmail, newEmail));
            
            mailSender.send(newEmailMessage);
            log.info("Email change confirmation sent to new email: {}", newEmail);
        } catch (Exception e) {
            log.error("Failed to send email change notifications: {}", e.getMessage(), e);
        }
    }

    private String buildOldEmailNotificationBody(String userName, String oldEmail, String newEmail) {
        return String.format(
            "Hello %s,\n\n" +
            "SECURITY NOTICE: The email address associated with your MRRG account has been changed.\n\n" +
            "Previous Email: %s\n" +
            "New Email: %s\n\n" +
            "If you did not authorize this change, please contact an administrator immediately.\n\n" +
            "Best regards,\n" +
            "MRRG Team",
            userName, oldEmail, newEmail
        );
    }

    private String buildNewEmailNotificationBody(String userName, String oldEmail, String newEmail) {
        return String.format(
            "Hello %s,\n\n" +
            "Your MRRG account email address has been successfully updated.\n\n" +
            "New Email: %s\n\n" +
            "This email address is now associated with your account. If you did not request this change, " +
            "please contact an administrator immediately.\n\n" +
            "Best regards,\n" +
            "MRRG Team",
            userName, newEmail
        );
    }
}
