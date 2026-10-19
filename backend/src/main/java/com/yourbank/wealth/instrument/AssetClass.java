package com.yourbank.wealth.instrument;

public enum AssetClass {
    EQUITY,
    BOND,
    FUND,
    ETF,
    OPTION,
    FUTURE;

    public boolean isDerivative() {
        return this == OPTION || this == FUTURE;
    }
}
