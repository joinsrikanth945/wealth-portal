package com.yourbank.wealth.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.yourbank.wealth.TestData;
import com.yourbank.wealth.instrument.AssetClass;
import com.yourbank.wealth.instrument.Instrument;
import com.yourbank.wealth.order.OrderEnums.Side;
import com.yourbank.wealth.order.PreTradeChecks.Check;
import com.yourbank.wealth.order.PreTradeChecks.Context;
import com.yourbank.wealth.order.PreTradeChecks.Outcome;
import com.yourbank.wealth.portfolio.ServiceType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PreTradeChecksTest {

    private final Instrument share = TestData.instrument(1, AssetClass.EQUITY, "100", null);     // risk class 4
    private final Instrument option = TestData.instrument(2, AssetClass.OPTION, "5", "100");    // risk 7, complex

    /** A healthy order: 10k buy in a 200k advisory portfolio with 50k cash, balanced profile. */
    private Context ok(Instrument i) {
        return new Context(ServiceType.ADVISORY, i, Side.BUY, BigDecimal.TEN,
                bd("10025"), bd("10000"), bd("50000"), BigDecimal.ZERO, BigDecimal.ZERO, bd("200000"), 3, false);
    }

    @Test
    void aHealthyOrderPassesEverything() {
        List<Check> checks = PreTradeChecks.evaluate(ok(share));

        assertThat(checks).allMatch(c -> c.outcome() == Outcome.PASS);
        assertThat(PreTradeChecks.anyFail(checks)).isFalse();
        assertThat(PreTradeChecks.anyWarn(checks)).isFalse();
    }

    @Test
    void noManualOrdersOnDiscretionaryPortfolios() {
        Context c = withService(ok(share), ServiceType.DISCRETIONARY);

        assertThat(PreTradeChecks.serviceType(c).outcome()).isEqualTo(Outcome.FAIL);
    }

    @Test
    void notEnoughCashFails() {
        Context c = new Context(ServiceType.ADVISORY, share, Side.BUY, BigDecimal.TEN,
                bd("10025"), bd("10000"), bd("5000"), BigDecimal.ZERO, BigDecimal.ZERO, bd("200000"), 3, false);

        assertThat(PreTradeChecks.cash(c).outcome()).isEqualTo(Outcome.FAIL);
    }

    @Test
    void sellingMoreSharesThanHeldFailsButDerivativesOnlyWarn() {
        Context shares = new Context(ServiceType.EXECUTION_ONLY, share, Side.SELL, BigDecimal.TEN,
                bd("985"), bd("1000"), bd("0"), bd("4"), bd("400"), bd("200000"), 3, true);
        Context options = new Context(ServiceType.EXECUTION_ONLY, option, Side.SELL, BigDecimal.TEN,
                bd("4975"), bd("5000"), bd("0"), bd("0"), bd("0"), bd("200000"), 5, true);

        assertThat(PreTradeChecks.holding(shares).outcome()).isEqualTo(Outcome.FAIL);
        assertThat(PreTradeChecks.holding(options).outcome()).isEqualTo(Outcome.WARN);
    }

    @Test
    void adviceNeedsAValidProfile() {
        Context noProfile = new Context(ServiceType.ADVISORY, share, Side.BUY, BigDecimal.TEN,
                bd("10025"), bd("10000"), bd("50000"), BigDecimal.ZERO, BigDecimal.ZERO, bd("200000"), null, false);
        Context executionOnly = withService(noProfile, ServiceType.EXECUTION_ONLY);

        assertThat(PreTradeChecks.profile(noProfile).outcome()).isEqualTo(Outcome.FAIL);
        assertThat(PreTradeChecks.profile(executionOnly).outcome()).isEqualTo(Outcome.WARN);
    }

    @Test
    void riskClassAboveTheProfileWarns() {
        // profile 3 allows up to risk class 5; the option is class 7
        Context c = new Context(ServiceType.ADVISORY, option, Side.BUY, BigDecimal.ONE,
                bd("515"), bd("500"), bd("50000"), BigDecimal.ZERO, BigDecimal.ZERO, bd("200000"), 3, true);

        assertThat(PreTradeChecks.riskMatch(c).outcome()).isEqualTo(Outcome.WARN);
    }

    @Test
    void complexProductsWithoutKnowledgeFailForAdviceAndWarnForExecutionOnly() {
        Context advisory = new Context(ServiceType.ADVISORY, option, Side.BUY, BigDecimal.ONE,
                bd("515"), bd("500"), bd("50000"), BigDecimal.ZERO, BigDecimal.ZERO, bd("200000"), 5, false);
        Context executionOnly = withService(advisory, ServiceType.EXECUTION_ONLY);

        assertThat(PreTradeChecks.appropriateness(advisory).outcome()).isEqualTo(Outcome.FAIL);
        assertThat(PreTradeChecks.appropriateness(executionOnly).outcome()).isEqualTo(Outcome.WARN);
    }

    @Test
    void goingAbove20PercentInOneInstrumentWarns() {
        // already 30k held + 15k buy = 45k of 200k = 22.5%
        Context c = new Context(ServiceType.ADVISORY, share, Side.BUY, bd("150"),
                bd("15037.50"), bd("15000"), bd("50000"), bd("300"), bd("30000"), bd("200000"), 3, false);

        Check check = PreTradeChecks.concentration(c);
        assertThat(check.outcome()).isEqualTo(Outcome.WARN);
        assertThat(check.message()).contains("22.5%");
    }

    private static Context withService(Context c, ServiceType type) {
        return new Context(type, c.instrument(), c.side(), c.quantity(), c.netAmount(), c.grossAmount(),
                c.cashAvailable(), c.quantityHeld(), c.positionValue(), c.portfolioValue(), c.riskScore(),
                c.complexProductsOk());
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
