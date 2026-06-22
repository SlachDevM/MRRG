package com.mrrg.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    @Column(name = "updated_at")
    private Long updatedAt;

    @Column(name = "fcm_token", columnDefinition = "TEXT")
    private String fcmToken;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;

    public User() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.enabled = false;
    }

    public User(String email, String password, String name, UserRole role) {
        this.email = email != null ? email.trim().toLowerCase() : null;
        this.password = password;
        this.name = name;
        this.role = role;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.enabled = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim().toLowerCase() : null;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    /**
     * Checks if the user has activated their account by setting a password.
     * Used to distinguish between:
     * - Never-activated users (pending invitation, deactivated before activation)
     * - Previously-activated users (can be reactivated after deactivation)
     *
     * @return true if user has set a password (has activated), false otherwise
     */
    public boolean hasActivatedAccount() {
        return password != null && !password.isEmpty();
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Computes the user's status based on enabled flag and presence of valid activation tokens.
     * This method is used for DTO responses and API consumers.
     *
     * Status rules:
     * - PENDING_ACTIVATION: User is enabled=false (hasn't activated yet)
     * - ACTIVE: User is enabled=true
     * - DISABLED: User is enabled=false but has a valid activation token (was explicitly disabled)
     *
     * Note: This method requires the AccountActivationTokenRepository to check for pending tokens.
     * For a simpler approach without external dependencies, rely on enabled flag alone:
     * - enabled=false && has_valid_token = PENDING_ACTIVATION
     * - enabled=true = ACTIVE
     * - enabled=false && no_valid_token = DISABLED
     *
     * @return the computed UserStatus
     */
    public UserStatus getStatus() {
        // Simplified computation based on enabled flag
        // In a real scenario, we'd also check for valid activation tokens to distinguish
        // between PENDING_ACTIVATION and DISABLED
        // For now, this is a placeholder that should be enhanced by the service layer
        if (enabled) {
            return UserStatus.ACTIVE;
        }
        // Default to DISABLED; the service layer will refine this if needed
        return UserStatus.DISABLED;
    }
}
