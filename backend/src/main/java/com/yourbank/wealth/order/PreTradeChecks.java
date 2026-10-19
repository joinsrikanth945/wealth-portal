package com.yourbank.wealth.order;

import com.yourbank.wealth.instrument.Instrument;
import com.yourbank.wealth.order.OrderEnums.Side;
import com.yourbank.wealth.portfolio.ServiceType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * The rules every order goes through before it can be placed. Pure logic on
 * a {@link Context}, so each rule is easy to unit-test.
 *
 * <ul>
 *   <li>FAIL — the order cannot be placed.</li>
 *   <li>WARN — the order can be placed but needs compliance approval.</li>
 * </ul>
 */
public final class PreTradeChecks {

    /** Largest share of the portfolio one instrument may reach without approval. */
    static final BigDecimal CONCENTRATION_LIMIT = BigDecimal.valueOf(20);

    private PreTradeChecks() {
    }

    public enum Outcome {
        PASS,
        WARN,
        FAIL
    }

    public record Check(String code, String label, Outcome outcome, String message) {
    }

    /** Everything the rules need to know about the order and the portfolio. */
    public record Context(
            ServiceType serviceType,
            Instrument instrument,
            Side side,
            BigDecimal quantity,
            BigDecimal netAmount,
            BigDecimal grossAmount,
            BigDecimal cashAvailable,
            BigDecimal quantityHeld,
            BigDecimal positionValue,
            BigDecimal portfolioValue,
            /** null when the client has no valid MiFID profile */
            Integer riskScore,
            boolean complexProductsOk) {
    }

    public static List<Check> evaluate(Context c) {
        List<Check> checks = new ArrayList<>();
        checks.add(serviceType(c));
        checks.add(c.side() == Side.BUY ? cash(c) : holding(c));
        checks.add(profile(c));
        checks.add(riskMatch(c));
        checks.add(appropriateness(c));
        checks.add(concentration(c));
        return checks;
    }

    static Check serviceType(Context c) {
        if (c.serviceType() == ServiceType.DISCRETIONARY) {
            return fail("SERVICE", "Service type",
                    "No manual orders on discretionary portfolios: the bank manages them.");
        }
        return pass("SERVICE", "Service type");
    }

    static Check cash(Context c) {
        if (c.netAmount().compareTo(c.cashAvailable()) > 0) {
            return fail("CASH", "Cash available",
                    "Needs " + c.netAmount().toPlainString() + " but only " + c.cashAvailable().toPlainString()
                            + " is available.");
        }
        return pass("CASH", "Cash available");
    }

    static Check holding(Context c) {
        BigDecimal held = c.quantityHeld();
        if (c.quantity().compareTo(held) <= 0) {
            return pass("HOLDING", "Position held");
        }
        if (c.instrument().getAssetClass().isDerivative()) {
            return warn("HOLDING", "Position held",
                    "Selling more contracts than held opens a short position.");
        }
        return fail("HOLDING", "Position held",
                "Only " + held.stripTrailingZeros().toPlainString() + " held; short selling is not allowed.");
    }

    static Check profile(Context c) {
        if (c.riskScore() != null) {
            return pass("PROFILE", "MiFID profile");
        }
        if (c.serviceType() == ServiceType.ADVISORY) {
            return fail("PROFILE", "MiFID profile",
                    "The client has no valid suitability profile, so no advice can be given.");
        }
        return warn("PROFILE", "MiFID profile", "The client has no valid MiFID profile.");
    }

    /** Profile 1..5 allows instruments up to risk class 3..7. */
    static Check riskMatch(Context c) {
        if (c.riskScore() == null || c.side() == Side.SELL) {
            return pass("RISK", "Risk profile match");
        }
        int maxClass = c.riskScore() + 2;
        if (c.instrument().getRiskClass() > maxClass) {
            return warn("RISK", "Risk profile match",
                    "Risk class " + c.instrument().getRiskClass() + " is above what profile " + c.riskScore()
                            + " allows (" + maxClass + ").");
        }
        return pass("RISK", "Risk profile match");
    }

    static Check appropriateness(Context c) {
        if (!c.instrument().isComplex() || c.complexProductsOk()) {
            return pass("APPROPRIATENESS", "Appropriateness (complex products)");
        }
        if (c.serviceType() == ServiceType.ADVISORY) {
            return fail("APPROPRIATENESS", "Appropriateness (complex products)",
                    "Complex product, and the client's profile does not show the knowledge needed.");
        }
        return warn("APPROPRIATENESS", "Appropriateness (complex products)",
                "Complex product the client may not understand; they must be warned before trading.");
    }

    static Check concentration(Context c) {
        if (c.side() == Side.SELL || c.portfolioValue().signum() == 0) {
            return pass("CONCENTRATION", "Concentration limit");
        }
        BigDecimal after = c.positionValue().add(c.grossAmount());
        BigDecimal percent = after.multiply(BigDecimal.valueOf(100)).divide(c.portfolioValue(), 1, RoundingMode.HALF_UP);
        if (percent.compareTo(CONCENTRATION_LIMIT) > 0) {
            return warn("CONCENTRATION", "Concentration limit",
                    c.instrument().getName() + " would be " + percent.toPlainString() + "% of the portfolio (limit "
                            + CONCENTRATION_LIMIT + "%).");
        }
        return pass("CONCENTRATION", "Concentration limit");
    }

    public static boolean anyFail(List<Check> checks) {
        return checks.stream().anyMatch(ch -> ch.outcome() == Outcome.FAIL);
    }

    public static boolean anyWarn(List<Check> checks) {
        return checks.stream().anyMatch(ch -> ch.outcome() == Outcome.WARN);
    }

    /** The messages of the given outcome, joined; null when there are none. */
    public static String summary(List<Check> checks, Outcome outcome) {
        List<String> parts = checks.stream()
                .filter(ch -> ch.outcome() == outcome)
                .map(ch -> ch.label() + ": " + ch.message())
                .toList();
        return parts.isEmpty() ? null : String.join(" | ", parts);
    }

    private static Check pass(String code, String label) {
        return new Check(code, label, Outcome.PASS, null);
    }

    private static Check warn(String code, String label, String message) {
        return new Check(code, label, Outcome.WARN, message);
    }

    private static Check fail(String code, String label, String message) {
        return new Check(code, label, Outcome.FAIL, message);
    }
}
