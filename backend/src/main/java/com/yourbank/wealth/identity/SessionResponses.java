package com.yourbank.wealth.identity;

import java.time.Instant;
import java.util.List;

/** JSON returned by the session endpoints. */
public final class SessionResponses {

    private SessionResponses() {
    }

    public record SessionStartResponse(Instant previousLoginAt, boolean firstLogin) {
    }

    public record MeResponse(
            String username,
            String displayName,
            String email,
            List<String> roles,
            Instant lastLoginAt) {
    }
}
