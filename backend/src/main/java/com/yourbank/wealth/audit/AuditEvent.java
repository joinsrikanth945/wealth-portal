package com.yourbank.wealth.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Who did what, and when. Rows are only ever inserted: the regulatory audit
 * trail must not be edited or deleted by the application.
 */
@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(nullable = false, updatable = false)
    private String actor;

    @Column(nullable = false, updatable = false, length = 64)
    private String action;

    @Column(updatable = false, length = 2000)
    private String detail;

    protected AuditEvent() {
        // for JPA
    }

    public AuditEvent(Instant occurredAt, String actor, String action, String detail) {
        this.occurredAt = occurredAt;
        this.actor = actor;
        this.action = action;
        this.detail = detail;
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public String getDetail() {
        return detail;
    }
}
