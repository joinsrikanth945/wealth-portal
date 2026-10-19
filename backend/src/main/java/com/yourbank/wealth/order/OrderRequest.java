package com.yourbank.wealth.order;

import com.yourbank.wealth.order.OrderEnums.LimitType;
import com.yourbank.wealth.order.OrderEnums.Side;
import com.yourbank.wealth.order.OrderEnums.TimeInForce;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/** What the order ticket sends, for both preview and placing. */
public record OrderRequest(
        @NotNull Long portfolioId,
        @NotNull Long instrumentId,
        @NotNull Side side,
        @NotNull @Positive BigDecimal quantity,
        @NotNull LimitType limitType,
        @Positive BigDecimal limitPrice,
        @Positive BigDecimal stopPrice,
        @NotNull TimeInForce timeInForce,
        LocalDate validUntil,
        /** A commission agreed with the client instead of the standard one. */
        @DecimalMin("0") BigDecimal negotiatedFee) {

    /** A plain market order, as created from an accepted proposal. */
    public static OrderRequest market(Long portfolioId, Long instrumentId, Side side, BigDecimal quantity) {
        return new OrderRequest(portfolioId, instrumentId, side, quantity, LimitType.MARKET, null, null,
                TimeInForce.DAY, null, null);
    }
}
