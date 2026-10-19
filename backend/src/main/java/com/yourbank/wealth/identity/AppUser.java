package com.yourbank.wealth.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Our own record of a person who has signed in. Keycloak remains the source
 * of truth for passwords and roles; this table links the Keycloak account to
 * bank data (clients, portfolios…) and keeps sign-in history.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "keycloak_id", nullable = false, unique = true, updatable = false)
    private String keycloakId;

    @Column(nullable = false)
    private String username;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected AppUser() {
        // for JPA
    }

    public AppUser(String keycloakId, String username, String displayName, String email, Instant createdAt) {
        this.keycloakId = keycloakId;
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.createdAt = createdAt;
    }

    /** Keeps name and email in step with Keycloak, which may have changed them. */
    public void refreshProfile(String username, String displayName, String email) {
        this.username = username;
        this.displayName = displayName;
        this.email = email;
    }

    /** Records a sign-in and returns the previous one (null the first time). */
    public Instant recordLogin(Instant now) {
        Instant previous = this.lastLoginAt;
        this.lastLoginAt = now;
        return previous;
    }

    public UUID getId() {
        return id;
    }

    public String getKeycloakId() {
        return keycloakId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }
}
