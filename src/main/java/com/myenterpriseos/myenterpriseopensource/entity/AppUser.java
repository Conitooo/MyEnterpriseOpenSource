package com.myenterpriseos.myenterpriseopensource.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import com.myenterpriseos.myenterpriseopensource.enums.*;

@Entity
@Table(name = "app_user")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private UserRole role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }

    public String getUsername() { return username; }
    public void setUsername(String value) { this.username = value; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String value) { this.passwordHash = value; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole value) { this.role = value; }

    public Company getCompany() { return company; }
    public void setCompany(Company value) { this.company = value; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }

    public boolean isActive() { return active; }
    public void setActive(boolean value) { this.active = value; }

    public int getTokenVersion() { return tokenVersion; }
    public void setTokenVersion(int value) { this.tokenVersion = value; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int value) { this.failedLoginAttempts = value; }

    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant value) { this.lockedUntil = value; }
}
