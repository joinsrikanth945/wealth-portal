package com.yourbank.wealth.order;

import com.yourbank.wealth.instrument.AssetClass;
import com.yourbank.wealth.order.OrderEnums.LimitType;
import com.yourbank.wealth.order.OrderEnums.Side;
import com.yourbank.wealth.order.OrderEnums.Status;
import com.yourbank.wealth.order.OrderEnums.TimeInForce;
import com.yourbank.wealth.order.PreTradeChecks.Check;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    /** The live result shown on the order ticket while the user types. */
    public record OrderPreview(
            Long instrumentId,
            String instrumentName,
            AssetClass assetClass,
            String natureLabel,
            BigDecimal lastPrice,
            BigDecimal estimatedPrice,
            BigDecimal grossAmount,
            BigDecimal fees,
            BigDecimal standardFees,
            BigDecimal netAmount,
            BigDecimal cashAvailable,
            BigDecimal quantityHeld,
            List<Check> checks,
            boolean canSubmit,
            boolean needsApproval) {
    }

    public record OrderDto(
            Long id,
            Long portfolioId,
            String portfolioNumber,
            String portfolioName,
            Long clientId,
            String clientName,
            Long instrumentId,
            String instrumentCode,
            String instrumentName,
            AssetClass assetClass,
            Side side,
            String natureLabel,
            BigDecimal quantity,
            LimitType limitType,
            BigDecimal limitPrice,
            BigDecimal stopPrice,
            TimeInForce timeInForce,
            LocalDate validUntil,
            Status status,
            BigDecimal estimatedPrice,
            BigDecimal grossAmount,
            BigDecimal fees,
            BigDecimal netAmount,
            BigDecimal executedPrice,
            Instant executedAt,
            String checkSummary,
            Long proposalId,
            String createdBy,
            Instant createdAt,
            String decidedBy,
            Instant decidedAt,
            String decisionNote) {

        static OrderDto from(TradeOrder o) {
            var p = o.getPortfolio();
            var i = o.getInstrument();
            return new OrderDto(
                    o.getId(), p.getId(), p.getPortfolioNumber(), p.getName(),
                    p.getClient().getId(), p.getClient().getFullName(),
                    i.getId(), i.getCode(), i.getName(), i.getAssetClass(),
                    o.getSide(), OrderDtos.natureLabel(i.getAssetClass(), o.getSide()), o.getQuantity().stripTrailingZeros(),
                    o.getLimitType(), o.getLimitPrice(), o.getStopPrice(), o.getTimeInForce(), o.getValidUntil(),
                    o.getStatus(), o.getEstimatedPrice(), o.getGrossAmount(), o.getFees(), o.getNetAmount(),
                    o.getExecutedPrice(), o.getExecutedAt(), o.getCheckSummary(), o.getProposalId(),
                    o.getCreatedBy(), o.getCreatedAt(), o.getDecidedBy(), o.getDecidedAt(), o.getDecisionNote());
        }
    }

    /** Derivatives say "Buy to open" / "Sell to open"; other instruments just "Buy" / "Sell". */
    static String natureLabel(AssetClass assetClass, Side side) {
        if (assetClass.isDerivative()) {
            return side == Side.BUY ? "Buy to open" : "Sell to open";
        }
        return side == Side.BUY ? "Buy" : "Sell";
    }

    public record DecisionRequest(String note) {
    }
}
