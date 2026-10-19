package com.yourbank.wealth.portfolio;

import com.yourbank.wealth.instrument.Instrument;
import com.yourbank.wealth.portfolio.PortfolioDtos.AllocationSlice;
import com.yourbank.wealth.portfolio.PortfolioDtos.PositionLine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Values a portfolio at the latest prices. Pure calculation, no database,
 * so it is easy to test. All amounts are in the portfolio currency.
 */
public record Valuation(
        BigDecimal totalValue,
        BigDecimal investedValue,
        BigDecimal cash,
        BigDecimal unrealizedPnl,
        BigDecimal cashRatioPercent,
        List<PositionLine> lines,
        List<AllocationSlice> allocation) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public static Valuation of(List<Position> positions, List<CashAccount> cashAccounts) {
        BigDecimal cash = cashAccounts.stream().map(CashAccount::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal invested = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        for (Position p : positions) {
            Instrument i = p.getInstrument();
            invested = invested.add(i.valueOf(p.getQuantity(), i.getLastPrice()));
            cost = cost.add(i.valueOf(p.getQuantity(), p.getAverageCost()));
        }
        BigDecimal total = invested.add(cash);

        final BigDecimal totalForWeights = total;
        List<PositionLine> lines = positions.stream()
                .filter(p -> p.getQuantity().signum() != 0)
                .map(p -> line(p, totalForWeights))
                .sorted(Comparator.comparing(PositionLine::marketValue).reversed())
                .toList();

        Map<String, BigDecimal> byClass = new LinkedHashMap<>();
        for (PositionLine line : lines) {
            byClass.merge(line.assetClass().name(), line.marketValue(), BigDecimal::add);
        }
        byClass.merge("CASH", cash, BigDecimal::add);
        List<AllocationSlice> allocation = byClass.entrySet().stream()
                .map(e -> new AllocationSlice(e.getKey(), money(e.getValue()), percent(e.getValue(), total)))
                .sorted(Comparator.comparing(AllocationSlice::value).reversed())
                .toList();

        return new Valuation(
                money(total), money(invested), money(cash), money(invested.subtract(cost)),
                percent(cash, total), lines, allocation);
    }

    private static PositionLine line(Position p, BigDecimal total) {
        Instrument i = p.getInstrument();
        BigDecimal market = i.valueOf(p.getQuantity(), i.getLastPrice());
        BigDecimal costValue = i.valueOf(p.getQuantity(), p.getAverageCost());
        BigDecimal pnl = market.subtract(costValue);
        return new PositionLine(
                p.getId(), i.getId(), i.getCode(), i.getName(), i.getAssetClass(), i.getRiskClass(), i.isComplex(),
                p.getQuantity().stripTrailingZeros(), p.getAverageCost(), i.getLastPrice(), i.getPriceDate(),
                money(market), money(costValue), money(pnl), percent(pnl, costValue), percent(market, total));
    }

    static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** part / whole as a percentage with 2 decimals; 0 when whole is 0. */
    static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return part.multiply(HUNDRED).divide(whole.abs(), 2, RoundingMode.HALF_UP);
    }
}
