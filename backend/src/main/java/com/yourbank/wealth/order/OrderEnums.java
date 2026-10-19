package com.yourbank.wealth.order;

/** Small value types used by orders. */
public final class OrderEnums {

    private OrderEnums() {
    }

    public enum Side {
        BUY,
        SELL
    }

    public enum LimitType {
        /** Executes at the best available price. */
        MARKET,
        /** Executes only at the limit price or better. */
        LIMIT,
        /** Becomes a market order once the stop price is reached. */
        STOP,
        /** Becomes a limit order once the stop price is reached. */
        STOP_LIMIT;

        public boolean needsLimitPrice() {
            return this == LIMIT || this == STOP_LIMIT;
        }

        public boolean needsStopPrice() {
            return this == STOP || this == STOP_LIMIT;
        }
    }

    public enum TimeInForce {
        DAY,
        /** Good till cancelled */
        GTC,
        /** Good till date */
        GTD
    }

    public enum Status {
        /** A pre-trade check raised a warning: compliance must approve. */
        PENDING_APPROVAL,
        /** Sent to the market, waiting for its price. */
        PLACED,
        FILLED,
        CANCELLED,
        REJECTED;

        public boolean isOpen() {
            return this == PENDING_APPROVAL || this == PLACED;
        }
    }
}
