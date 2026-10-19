package com.yourbank.wealth.portfolio;

import com.yourbank.wealth.instrument.Instrument;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** A holding of one instrument in one portfolio. */
@Entity
@Table(name = "position")
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    /** Units held; for bonds, the nominal amount. */
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    /** Average purchase price per unit (bonds: % of nominal). */
    @Column(name = "average_cost", nullable = false, precision = 19, scale = 6)
    private BigDecimal averageCost;

    protected Position() {
        // for JPA
    }

    public Position(Portfolio portfolio, Instrument instrument, BigDecimal quantity, BigDecimal averageCost) {
        this.portfolio = portfolio;
        this.instrument = instrument;
        this.quantity = quantity;
        this.averageCost = averageCost;
    }

    /** Adds a purchase, re-averaging the cost price. */
    public void buy(BigDecimal qty, BigDecimal price) {
        BigDecimal totalCost = quantity.multiply(averageCost).add(qty.multiply(price));
        quantity = quantity.add(qty);
        averageCost = quantity.signum() == 0 ? price : totalCost.divide(quantity, 6, RoundingMode.HALF_UP);
    }

    /** Removes a sale; the cost price of what remains is unchanged. */
    public void sell(BigDecimal qty) {
        quantity = quantity.subtract(qty);
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

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getAverageCost() {
        return averageCost;
    }
}
