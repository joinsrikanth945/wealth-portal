package com.yourbank.wealth.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.yourbank.wealth.TestData;
import com.yourbank.wealth.instrument.AssetClass;
import com.yourbank.wealth.instrument.Instrument;
import com.yourbank.wealth.order.OrderEnums.Side;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderCostsTest {

    @Test
    void sharesPayAQuarterPercentWithAFifteenMinimum() {
        Instrument share = TestData.instrument(1, AssetClass.EQUITY, "100", null);

        OrderCosts big = OrderCosts.of(share, Side.BUY, new BigDecimal("100"), new BigDecimal("100"), null);
        OrderCosts small = OrderCosts.of(share, Side.BUY, new BigDecimal("2"), new BigDecimal("100"), null);

        assertThat(big.gross()).isEqualByComparingTo("10000.00");
        assertThat(big.fees()).isEqualByComparingTo("25.00");
        assertThat(big.net()).isEqualByComparingTo("10025.00");
        assertThat(small.fees()).isEqualByComparingTo("15.00");
    }

    @Test
    void derivativesPayPerContractAndUseTheContractSize() {
        Instrument option = TestData.instrument(2, AssetClass.OPTION, "5", "100");

        OrderCosts c = OrderCosts.of(option, Side.BUY, new BigDecimal("10"), new BigDecimal("5"), null);

        assertThat(c.gross()).isEqualByComparingTo("5000.00");   // 10 x 5 x 100
        assertThat(c.fees()).isEqualByComparingTo("25.00");      // 10 x 2.50
    }

    @Test
    void aSaleReceivesGrossMinusFeesAndANegotiatedFeeWins() {
        Instrument share = TestData.instrument(1, AssetClass.EQUITY, "100", null);

        OrderCosts c = OrderCosts.of(share, Side.SELL, new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("9.90"));

        assertThat(c.fees()).isEqualByComparingTo("9.90");
        assertThat(c.standardFees()).isEqualByComparingTo("25.00");
        assertThat(c.net()).isEqualByComparingTo("9990.10");
    }

    @Test
    void bondsArePricedInPercentOfNominal() {
        Instrument bond = TestData.instrument(3, AssetClass.BOND, "98", null);

        OrderCosts c = OrderCosts.of(bond, Side.BUY, new BigDecimal("10000"), new BigDecimal("98"), null);

        assertThat(c.gross()).isEqualByComparingTo("9800.00");
        assertThat(c.fees()).isEqualByComparingTo("24.50");
    }
}
