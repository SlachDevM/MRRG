package com.mrrg.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "account_activation_tokens")
public class AccountActivationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "expires_at", nullable = false)
    private Long expiresAt;

    @Column(name = "used_at")
    private Long usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    public AccountActivationToken() {
        this.createdAt = System.currentTimeMillis();
    }

    public AccountActivationToken(String token, User user, Long expiresAt) {
        this.token = token;
        this.user = user;
        this.expiresAt = expiresAt;
        this.createdAt = System.currentTimeMillis();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @JsonIgnore
    public User getUser() {
        return user;
    }

    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Long usedAt) {
        this.usedAt = usedAt;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isValid() {
        return !isExpired() && !isUsed();
    }
}
