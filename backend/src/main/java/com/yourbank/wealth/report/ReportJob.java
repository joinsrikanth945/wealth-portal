package com.yourbank.wealth.report;

import com.yourbank.wealth.portfolio.Portfolio;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** One requested report. Generated in the background: PENDING, then COMPLETED or FAILED. */
@Entity
@Table(name = "report_job")
public class ReportJob {

    public enum Type {
        STATEMENT("Client statement"),
        VALUATION("Portfolio valuation"),
        PERFORMANCE("Performance report"),
        TRANSACTIONS("Transaction history"),
        COSTS("Ex-post costs and charges");

        private final String title;

        Type(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }

    public enum Status {
        PENDING,
        COMPLETED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false, updatable = false)
    private Portfolio portfolio;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 20)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Status status;

    @Column(name = "requested_by", nullable = false, length = 100, updatable = false)
    private String requestedBy;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "file_name", length = 200)
    private String fileName;

    /** The PDF, base64-encoded; only loaded when downloading. */
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "content_base64")
    private String contentBase64;

    @Column(length = 1000)
    private String error;

    protected ReportJob() {
        // for JPA
    }

    public ReportJob(Portfolio portfolio, Type type, String requestedBy, Instant now) {
        this.portfolio = portfolio;
        this.type = type;
        this.status = Status.PENDING;
        this.requestedBy = requestedBy;
        this.requestedAt = now;
    }

    void complete(String fileName, String base64, Instant now) {
        this.status = Status.COMPLETED;
        this.fileName = fileName;
        this.contentBase64 = base64;
        this.completedAt = now;
    }

    void fail(String message, Instant now) {
        this.status = Status.FAILED;
        this.error = message == null ? "Unknown error" : message.substring(0, Math.min(message.length(), 1000));
        this.completedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public Type getType() {
        return type;
    }

    public Status getStatus() {
        return status;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentBase64() {
        return contentBase64;
    }

    public String getError() {
        return error;
    }
}
