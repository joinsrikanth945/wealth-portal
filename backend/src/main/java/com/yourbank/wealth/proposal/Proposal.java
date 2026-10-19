package com.yourbank.wealth.proposal;

import com.yourbank.wealth.instrument.Instrument;
import com.yourbank.wealth.order.OrderEnums.Side;
import com.yourbank.wealth.portfolio.Portfolio;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "proposal")
public class Proposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false, updatable = false)
    private Portfolio portfolio;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 4000)
    private String rationale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ProposalStatus status;

    @Column(name = "created_by", nullable = false, length = 100, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decided_by", length = 100)
    private String decidedBy;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "signature_png")
    private String signaturePng;

    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo")
    private List<ProposalLine> lines = new ArrayList<>();

    protected Proposal() {
        // for JPA
    }

    public Proposal(Portfolio portfolio, String title, String rationale, String createdBy, Instant now) {
        this.portfolio = portfolio;
        this.title = title;
        this.rationale = rationale;
        this.status = ProposalStatus.WORKING;
        this.createdBy = createdBy;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Replaces title, rationale and lines. A proposal already sent goes back to WORKING. */
    void edit(String newTitle, String newRationale, List<LineSpec> newLines, Instant now) {
        title = newTitle;
        rationale = newRationale;
        lines.clear();
        int n = 1;
        for (LineSpec l : newLines) {
            lines.add(new ProposalLine(this, n++, l.instrument(), l.side(), l.quantity(), l.comment()));
        }
        if (status == ProposalStatus.SENT_FOR_VALIDATION) {
            status = ProposalStatus.WORKING;
            sentAt = null;
        }
        updatedAt = now;
    }

    record LineSpec(Instrument instrument, Side side, BigDecimal quantity, String comment) {
    }

    void send(Instant now) {
        status = ProposalStatus.SENT_FOR_VALIDATION;
        sentAt = now;
        updatedAt = now;
    }

    void validate(String signature, String by, Instant now) {
        status = ProposalStatus.VALIDATED;
        signaturePng = signature;
        decidedBy = by;
        decidedAt = now;
        updatedAt = now;
    }

    void reject(String reason, String by, Instant now) {
        status = ProposalStatus.REJECTED;
        rejectReason = reason;
        decidedBy = by;
        decidedAt = now;
        updatedAt = now;
    }

    void markExecuted(Instant now) {
        status = ProposalStatus.EXECUTED;
        updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public String getTitle() {
        return title;
    }

    public String getRationale() {
        return rationale;
    }

    public ProposalStatus getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public String getSignaturePng() {
        return signaturePng;
    }

    public List<ProposalLine> getLines() {
        return lines;
    }
}
