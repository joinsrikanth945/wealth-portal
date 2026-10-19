package com.yourbank.wealth.order;

import com.yourbank.wealth.instrument.Instrument;
import com.yourbank.wealth.order.OrderEnums.LimitType;
import com.yourbank.wealth.order.OrderEnums.Side;
import com.yourbank.wealth.order.OrderEnums.Status;
import com.yourbank.wealth.order.OrderEnums.TimeInForce;
import com.yourbank.wealth.portfolio.Portfolio;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "trade_order")
public class TradeOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false, updatable = false)
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false, updatable = false)
    private Instrument instrument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private Side side;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_type", nullable = false, length = 12)
    private LimitType limitType;

    @Column(name = "limit_price", precision = 19, scale = 6)
    private BigDecimal limitPrice;

    @Column(name = "stop_price", precision = 19, scale = 6)
    private BigDecimal stopPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_in_force", nullable = false, length = 4)
    private TimeInForce timeInForce;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "estimated_price", nullable = false, precision = 19, scale = 6)
    private BigDecimal estimatedPrice;

    @Column(name = "gross_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal fees;

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "negotiated_fee", precision = 19, scale = 2)
    private BigDecimal negotiatedFee;

    @Column(name = "executed_price", precision = 19, scale = 6)
    private BigDecimal executedPrice;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "check_summary", length = 2000)
    private String checkSummary;

    @Column(name = "proposal_id")
    private Long proposalId;

    @Column(name = "created_by", nullable = false, length = 100, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "decided_by", length = 100)
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_note", length = 500)
    private String decisionNote;

    protected TradeOrder() {
        // for JPA
    }

    public TradeOrder(Portfolio portfolio, Instrument instrument, OrderRequest r, OrderCosts costs,
                      String checkSummary, Long proposalId, String createdBy, Instant now) {
        this.portfolio = portfolio;
        this.instrument = instrument;
        this.side = r.side();
        this.quantity = r.quantity();
        this.limitType = r.limitType();
        this.limitPrice = r.limitPrice();
        this.stopPrice = r.stopPrice();
        this.timeInForce = r.timeInForce();
        this.validUntil = r.validUntil();
        this.estimatedPrice = costs.price();
        this.grossAmount = costs.gross();
        this.fees = costs.fees();
        this.netAmount = costs.net();
        this.negotiatedFee = r.negotiatedFee();
        this.checkSummary = checkSummary;
        this.proposalId = proposalId;
        this.createdBy = createdBy;
        this.createdAt = now;
        this.status = checkSummary == null ? Status.PLACED : Status.PENDING_APPROVAL;
    }

    void markPlaced() {
        status = Status.PLACED;
    }

    void fill(BigDecimal price, OrderCosts costsAtFill, Instant now) {
        status = Status.FILLED;
        executedPrice = price;
        executedAt = now;
        grossAmount = costsAtFill.gross();
        fees = costsAtFill.fees();
        netAmount = costsAtFill.net();
    }

    void decide(Status newStatus, String by, String note, Instant now) {
        status = newStatus;
        decidedBy = by;
        decisionNote = note;
        decidedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public Side getSide() {
        return side;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public LimitType getLimitType() {
        return limitType;
    }

    public BigDecimal getLimitPrice() {
        return limitPrice;
    }

    public BigDecimal getStopPrice() {
        return stopPrice;
    }

    public TimeInForce getTimeInForce() {
        return timeInForce;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public Status getStatus() {
        return status;
    }

    public BigDecimal getEstimatedPrice() {
        return estimatedPrice;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public BigDecimal getFees() {
        return fees;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public BigDecimal getNegotiatedFee() {
        return negotiatedFee;
    }

    public BigDecimal getExecutedPrice() {
        return executedPrice;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public String getCheckSummary() {
        return checkSummary;
    }

    public Long getProposalId() {
        return proposalId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public String getDecisionNote() {
        return decisionNote;
    }
}
