package com.yourbank.wealth.proposal;

public enum ProposalStatus {
    /** Being prepared by the RM; editable, deletable. */
    WORKING,
    /** Sent to the client, waiting for their decision. Editing returns it to WORKING. */
    SENT_FOR_VALIDATION,
    /** Accepted and signed by the client; ready to be turned into orders. */
    VALIDATED,
    /** Declined by the client. */
    REJECTED,
    /** Its orders have been placed. */
    EXECUTED
}
