package com.yourbank.wealth.portfolio;

/**
 * How the bank looks after a portfolio. It decides who may place orders:
 * no manual orders on discretionary portfolios (the bank decides).
 */
public enum ServiceType {
    ADVISORY,
    DISCRETIONARY,
    EXECUTION_ONLY
}
