package com.yourbank.wealth.portfolio;

import static org.assertj.core.api.Assertions.assertThat;

import com.yourbank.wealth.TestData;
import com.yourbank.wealth.instrument.AssetClass;
import com.yourbank.wealth.instrument.Instrument;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValuationTest {

    private final Portfolio portfolio = TestData.portfolio(1, TestData.client(1, "rm1", null), ServiceType.ADVISORY);

    @Test
    void valuesSharesBondsAndDerivatives() {
        Instrument share = TestData.instrument(1, AssetClass.EQUITY, "100", null);
        Instrument bond = TestData.instrument(2, AssetClass.BOND, "98.5", null);        // % of nominal
        Instrument option = TestData.instrument(3, AssetClass.OPTION, "2.5", "100");   // x contract size

        Valuation v = Valuation.of(List.of(
                        TestData.position(portfolio, share, "10", "80"),          // 1,000 (cost 800)
                        TestData.position(portfolio, bond, "10000", "100"),       // 9,850 (cost 10,000)
                        TestData.position(portfolio, option, "4", "2")),          // 1,000 (cost 800)
                List.of(TestData.cash(portfolio, "8150")));

        assertThat(v.investedValue()).isEqualByComparingTo("11850.00");
        assertThat(v.cash()).isEqualByComparingTo("8150.00");
        assertThat(v.totalValue()).isEqualByComparingTo("20000.00");
        assertThat(v.unrealizedPnl()).isEqualByComparingTo("250.00");  // +200 -150 +200
        assertThat(v.cashRatioPercent()).isEqualByComparingTo("40.75");
    }

    @Test
    void buildsAllocationIncludingCash() {
        Instrument share = TestData.instrument(1, AssetClass.EQUITY, "50", null);
        Valuation v = Valuation.of(
                List.of(TestData.position(portfolio, share, "60", "50")),
                List.of(TestData.cash(portfolio, "1000")));

        assertThat(v.allocation()).hasSize(2);
        assertThat(v.allocation().getFirst().assetClass()).isEqualTo("EQUITY");
        assertThat(v.allocation().getFirst().weightPercent()).isEqualByComparingTo("75.00");
        assertThat(v.allocation().get(1).assetClass()).isEqualTo("CASH");
    }

    @Test
    void emptyPortfolioDoesNotDivideByZero() {
        Valuation v = Valuation.of(List.of(), List.of());

        assertThat(v.totalValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(v.cashRatioPercent()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void reAveragesCostOnPurchase() {
        Instrument share = TestData.instrument(1, AssetClass.EQUITY, "50", null);
        Position p = TestData.position(portfolio, share, "10", "40");

        p.buy(new BigDecimal("10"), new BigDecimal("60"));

        assertThat(p.getQuantity()).isEqualByComparingTo("20");
        assertThat(p.getAverageCost()).isEqualByComparingTo("50");
    }
}
