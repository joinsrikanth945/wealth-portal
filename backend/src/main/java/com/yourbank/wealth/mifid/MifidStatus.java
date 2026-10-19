package com.yourbank.wealth.mifid;

public enum MifidStatus {
    /** Being filled in by the RM; can be saved and resumed. */
    IN_PROGRESS,
    /** Complete, waiting for the client to accept or refuse. */
    PENDING_VALIDATION,
    /** Accepted by the client: the profile in force until it expires. */
    VALID,
    /** An older valid profile replaced by a newer one. */
    SUPERSEDED,
    /** The client refused it; their previous profile stays in force. */
    REFUSED,
    /** Abandoned by the RM. */
    CANCELLED
}
