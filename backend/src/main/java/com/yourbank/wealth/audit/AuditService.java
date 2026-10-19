package com.yourbank.wealth.audit;

import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The one place every module writes audit entries through. */
@Service
public class AuditService {

    private final AuditEventRepository events;
    private final Clock clock;

    public AuditService(AuditEventRepository events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public void record(String actor, String action, String detail) {
        events.save(new AuditEvent(clock.instant(), actor, action, detail));
    }
}
