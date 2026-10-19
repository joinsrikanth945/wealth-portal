package com.yourbank.wealth.portfolio;

import com.yourbank.wealth.instrument.AssetClass;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** JSON shapes returned by the portfolio endpoints. */
public final class PortfolioDtos {

    private PortfolioDtos() {
    }

    /** A portfolio in a list: enough for a row in a table. */
    public record PortfolioSummary(
            Long id,
            String portfolioNumber,
            String name,
            ServiceType serviceType,
            String currency,
            BigDecimal totalValue,
            BigDecimal cash,
            BigDecimal cashRatioPercent) {
    }

    public record PositionLine(
            Long positionId,
            Long instrumentId,
            String instrumentCode,
            String instrumentName,
            AssetClass assetClass,
            int riskClass,
            boolean complex,
            BigDecimal quantity,
            BigDecimal averageCost,
            BigDecimal lastPrice,
            LocalDate priceDate,
            BigDecimal marketValue,
            BigDecimal costValue,
            BigDecimal unrealizedPnl,
            BigDecimal unrealizedPnlPercent,
            BigDecimal weightPercent) {
    }

    /** One slice of the allocation chart. "CASH" is included as its own slice. */
    public record AllocationSlice(String assetClass, BigDecimal value, BigDecimal weightPercent) {
    }

    public record HistoryPoint(LocalDate asOf, BigDecimal indexValue) {
    }

    public record PortfolioDetail(
            Long id,
            String portfolioNumber,
            String name,
            ServiceType serviceType,
            String currency,
            LocalDate openedOn,
            Long clientId,
            String clientName,
            String clientNumber,
            BigDecimal totalValue,
            BigDecimal investedValue,
            BigDecimal cash,
            BigDecimal cashRatioPercent,
            BigDecimal unrealizedPnl,
            BigDecimal performancePercent,
            List<AllocationSlice> allocation,
            List<PositionLine> positions,
            List<HistoryPoint> history) {
    }
}
