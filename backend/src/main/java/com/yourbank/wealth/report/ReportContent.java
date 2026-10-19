package com.yourbank.wealth.report;

import com.yourbank.wealth.order.OrderEnums.Status;
import com.yourbank.wealth.order.TradeOrder;
import com.yourbank.wealth.order.TradeOrderRepository;
import com.yourbank.wealth.portfolio.Portfolio;
import com.yourbank.wealth.portfolio.PortfolioDtos.AllocationSlice;
import com.yourbank.wealth.portfolio.PortfolioDtos.HistoryPoint;
import com.yourbank.wealth.portfolio.PortfolioDtos.PortfolioDetail;
import com.yourbank.wealth.portfolio.PortfolioDtos.PositionLine;
import com.yourbank.wealth.portfolio.PortfolioRepository;
import com.yourbank.wealth.portfolio.PortfolioService;
import com.yourbank.wealth.shared.pdf.PdfBuilder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Builds the PDF for each report type from live portfolio data. */
@Component
public class ReportContent {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private final PortfolioRepository portfolioRepo;
    private final PortfolioService portfolios;
    private final TradeOrderRepository orders;
    private final Clock clock;

    public ReportContent(PortfolioRepository portfolioRepo, PortfolioService portfolios,
                         TradeOrderRepository orders, Clock clock) {
        this.portfolioRepo = portfolioRepo;
        this.portfolios = portfolios;
        this.orders = orders;
        this.clock = clock;
    }

    /** File name and PDF bytes for one report. */
    public record Built(String fileName, byte[] pdf) {
    }

    @Transactional(readOnly = true)
    public Built build(Long portfolioId, ReportJob.Type type) {
        Portfolio portfolio = portfolioRepo.findById(portfolioId).orElseThrow();
        PortfolioDetail d = portfolios.detailOf(portfolio);
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        List<TradeOrder> filled = orders.findByPortfolioIds(List.of(portfolioId)).stream()
                .filter(o -> o.getStatus() == Status.FILLED)
                .toList();

        PdfBuilder pdf = new PdfBuilder(type.title(),
                d.clientName() + "  ·  " + d.portfolioNumber() + " " + d.name() + "  ·  as of " + DAY.format(today));
        pdf.keyValues(List.of(
                new String[] {"Client", d.clientName() + " (" + d.clientNumber() + ")"},
                new String[] {"Portfolio", d.portfolioNumber() + " - " + d.name()},
                new String[] {"Service", d.serviceType().name().replace('_', ' ').toLowerCase(Locale.ROOT)},
                new String[] {"Reference currency", d.currency()}));

        switch (type) {
            case STATEMENT -> statement(pdf, d, filled);
            case VALUATION -> valuation(pdf, d);
            case PERFORMANCE -> performance(pdf, d);
            case TRANSACTIONS -> transactions(pdf, filled);
            case COSTS -> costs(pdf, d, filled, today);
        }

        String fileName = type.name().toLowerCase(Locale.ROOT) + "-" + d.portfolioNumber() + "-" + today + ".pdf";
        return new Built(fileName, pdf.toBytes());
    }

    private void statement(PdfBuilder pdf, PortfolioDetail d, List<TradeOrder> filled) {
        pdf.heading("Summary").keyValues(List.of(
                new String[] {"Total value", money(d.totalValue())},
                new String[] {"Invested", money(d.investedValue())},
                new String[] {"Cash", money(d.cash()) + "  (" + d.cashRatioPercent() + "%)"},
                new String[] {"Unrealised gain / loss", money(d.unrealizedPnl())},
                new String[] {"12-month performance", signed(d.performancePercent()) + "%"}));
        holdingsTable(pdf, d.positions());
        pdf.total("Total value", money(d.totalValue()));
        pdf.heading("Recent transactions");
        transactionsTable(pdf, filled.stream().limit(10).toList());
    }

    private void valuation(PdfBuilder pdf, PortfolioDetail d) {
        holdingsTable(pdf, d.positions());
        pdf.total("Invested", money(d.investedValue()));
        pdf.heading("Asset allocation");
        List<String[]> rows = new ArrayList<>();
        for (AllocationSlice a : d.allocation()) {
            rows.add(new String[] {a.assetClass(), money(a.value()), a.weightPercent() + "%"});
        }
        pdf.table(new String[] {"Asset class", "Value", "Weight"}, new float[] {0.5f, 0.3f, 0.2f},
                new boolean[] {false, true, true}, rows);
        pdf.total("Total value", money(d.totalValue()));
    }

    private void performance(PdfBuilder pdf, PortfolioDetail d) {
        pdf.heading("12-month performance").keyValues(List.of(
                new String[] {"Performance over the period", signed(d.performancePercent()) + "%"},
                new String[] {"Method", "Time-weighted, month-end index (100 = start)"}));
        List<String[]> rows = new ArrayList<>();
        BigDecimal previous = null;
        for (HistoryPoint h : d.history()) {
            String change = previous == null ? "" : signed(h.indexValue().subtract(previous)
                    .multiply(BigDecimal.valueOf(100)).divide(previous, 2, RoundingMode.HALF_UP)) + "%";
            rows.add(new String[] {MONTH.format(h.asOf()), h.indexValue().setScale(2, RoundingMode.HALF_UP).toPlainString(), change});
            previous = h.indexValue();
        }
        pdf.table(new String[] {"Month end", "Index", "Monthly change"}, new float[] {0.4f, 0.3f, 0.3f},
                new boolean[] {false, true, true}, rows);
        pdf.paragraph("Past performance is not a reliable indicator of future results.");
    }

    private void transactions(PdfBuilder pdf, List<TradeOrder> filled) {
        pdf.heading("Executed orders");
        transactionsTable(pdf, filled);
    }

    private void costs(PdfBuilder pdf, PortfolioDetail d, List<TradeOrder> filled, LocalDate today) {
        Instant from = today.minusYears(1).atStartOfDay(ZONE).toInstant();
        List<TradeOrder> period = filled.stream()
                .filter(o -> o.getExecutedAt() != null && o.getExecutedAt().isAfter(from))
                .toList();
        BigDecimal fees = period.stream().map(TradeOrder::getFees).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal percent = d.totalValue().signum() == 0 ? BigDecimal.ZERO
                : fees.multiply(BigDecimal.valueOf(100)).divide(d.totalValue(), 3, RoundingMode.HALF_UP);
        pdf.heading("Costs over the last 12 months (MiFID II ex-post)").keyValues(List.of(
                new String[] {"Period", DAY.format(today.minusYears(1)) + " - " + DAY.format(today)},
                new String[] {"Transaction costs", money(fees)},
                new String[] {"Service costs", money(BigDecimal.ZERO)},
                new String[] {"Total costs", money(fees)},
                new String[] {"As % of current portfolio value", percent.toPlainString() + "%"}));
        List<String[]> rows = new ArrayList<>();
        for (TradeOrder o : period) {
            rows.add(new String[] {DAY.format(o.getExecutedAt().atZone(ZONE)), o.getSide().name(),
                    o.getInstrument().getName(), money(o.getGrossAmount()), money(o.getFees())});
        }
        pdf.heading("Transaction costs by order").table(
                new String[] {"Date", "Side", "Instrument", "Amount", "Costs"},
                new float[] {0.16f, 0.08f, 0.44f, 0.18f, 0.14f},
                new boolean[] {false, false, false, true, true}, rows);
        pdf.total("Total costs", money(fees));
        pdf.paragraph("Costs reduce the return on your investment. Fund ongoing charges are deducted inside each fund "
                + "and are not included above.");
    }

    private void holdingsTable(PdfBuilder pdf, List<PositionLine> positions) {
        List<String[]> rows = new ArrayList<>();
        for (PositionLine l : positions) {
            rows.add(new String[] {l.instrumentName(), l.assetClass().name(), plain(l.quantity()),
                    l.lastPrice().setScale(2, RoundingMode.HALF_UP).toPlainString(), money(l.marketValue()),
                    signed(l.unrealizedPnlPercent()) + "%", l.weightPercent() + "%"});
        }
        pdf.heading("Holdings").table(
                new String[] {"Instrument", "Type", "Quantity", "Price", "Value", "Gain/loss", "Weight"},
                new float[] {0.34f, 0.11f, 0.11f, 0.1f, 0.14f, 0.1f, 0.1f},
                new boolean[] {false, false, true, true, true, true, true}, rows);
    }

    private void transactionsTable(PdfBuilder pdf, List<TradeOrder> list) {
        List<String[]> rows = new ArrayList<>();
        for (TradeOrder o : list) {
            rows.add(new String[] {DAY.format(o.getExecutedAt().atZone(ZONE)), o.getSide().name(),
                    o.getInstrument().getName(), plain(o.getQuantity()),
                    o.getExecutedPrice().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    money(o.getNetAmount())});
        }
        pdf.table(new String[] {"Date", "Side", "Instrument", "Quantity", "Price", "Net amount"},
                new float[] {0.15f, 0.08f, 0.4f, 0.11f, 0.11f, 0.15f},
                new boolean[] {false, false, false, true, true, true}, rows);
    }

    private static String money(BigDecimal v) {
        return String.format(Locale.ENGLISH, "EUR %,.2f", v);
    }

    private static String signed(BigDecimal v) {
        return (v.signum() >= 0 ? "+" : "") + v.toPlainString();
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
