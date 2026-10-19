package com.yourbank.wealth.order;

import com.yourbank.wealth.instrument.Instrument;
import com.yourbank.wealth.order.OrderEnums.Side;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Price, gross amount, fees and net amount of an order.
 *
 * <p>Standard fees: securities 0.25% of the gross amount, derivatives 2.50 per
 * contract, minimum 15 in both cases. A negotiated fee replaces the standard
 * one. For a buy the net amount is what the client pays; for a sell it is what
 * they receive.
 */
public record OrderCosts(BigDecimal price, BigDecimal gross, BigDecimal fees, BigDecimal standardFees, BigDecimal net) {

    static final BigDecimal SECURITIES_RATE = new BigDecimal("0.0025");
    static final BigDecimal PER_CONTRACT = new BigDecimal("2.50");
    static final BigDecimal MINIMUM = new BigDecimal("15.00");

    public static OrderCosts of(Instrument instrument, Side side, BigDecimal quantity, BigDecimal price,
                                BigDecimal negotiatedFee) {
        BigDecimal gross = money(instrument.valueOf(quantity, price));
        BigDecimal standard = instrument.getAssetClass().isDerivative()
                ? quantity.multiply(PER_CONTRACT)
                : gross.multiply(SECURITIES_RATE);
        standard = money(standard.max(MINIMUM));
        BigDecimal fees = negotiatedFee != null ? money(negotiatedFee) : standard;
        BigDecimal net = side == Side.BUY ? gross.add(fees) : gross.subtract(fees);
        return new OrderCosts(price, gross, fees, standard, net);
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
