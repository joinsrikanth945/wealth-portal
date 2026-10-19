package com.yourbank.wealth.identity;

import com.yourbank.wealth.audit.AuditService;
import com.yourbank.wealth.identity.SessionResponses.MeResponse;
import com.yourbank.wealth.identity.SessionResponses.SessionStartResponse;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private final AppUserRepository users;
    private final LoginEventRepository loginEvents;
    private final AuditService audit;
    private final Clock clock;

    public SessionService(AppUserRepository users, LoginEventRepository loginEvents, AuditService audit, Clock clock) {
        this.users = users;
        this.loginEvents = loginEvents;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * Called by the Angular app once per sign-in. Creates our user record on
     * the first visit, records the sign-in, and returns the previous one so
     * the user can spot activity they don't recognise.
     */
    @Transactional
    public SessionStartResponse startSession(CurrentUser current, String ipAddress, String userAgent) {
        Instant now = clock.instant();
        AppUser user = users.findByKeycloakId(current.keycloakId())
                .orElseGet(() -> users.save(new AppUser(
                        current.keycloakId(), current.username(), current.displayName(), current.email(), now)));
        user.refreshProfile(current.username(), current.displayName(), current.email());

        Instant previous = user.recordLogin(now);
        loginEvents.save(new LoginEvent(user, now, ipAddress, userAgent));
        audit.record(current.username(), "LOGIN", "Signed in from " + ipAddress);

        return new SessionStartResponse(previous, previous == null);
    }

    @Transactional(readOnly = true)
    public MeResponse me(CurrentUser current) {
        Instant lastLogin = users.findByKeycloakId(current.keycloakId())
                .map(AppUser::getLastLoginAt)
                .orElse(null);
        return new MeResponse(
                current.username(), current.displayName(), current.email(), current.roles(), lastLogin);
    }
}
