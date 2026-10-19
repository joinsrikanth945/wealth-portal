package com.yourbank.wealth.proposal;

import com.yourbank.wealth.instrument.Instrument;
import com.yourbank.wealth.order.OrderEnums.Side;
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

/** One suggested trade in a proposal. */
@Entity
@Table(name = "proposal_line")
public class ProposalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposal_id", nullable = false)
    private Proposal proposal;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private Side side;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(length = 500)
    private String comment;

    protected ProposalLine() {
        // for JPA
    }

    ProposalLine(Proposal proposal, int lineNo, Instrument instrument, Side side, BigDecimal quantity, String comment) {
        this.proposal = proposal;
        this.lineNo = lineNo;
        this.instrument = instrument;
        this.side = side;
        this.quantity = quantity;
        this.comment = comment;
    }

    public Long getId() {
        return id;
    }

    public Proposal getProposal() {
        return proposal;
    }

    public Integer getLineNo() {
        return lineNo;
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

    public String getComment() {
        return comment;
    }
}
