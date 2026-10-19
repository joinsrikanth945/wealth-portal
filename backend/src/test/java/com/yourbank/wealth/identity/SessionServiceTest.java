package com.yourbank.wealth.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yourbank.wealth.audit.AuditService;
import com.yourbank.wealth.identity.SessionResponses.SessionStartResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-19T09:00:00Z");
    private static final CurrentUser ALICE =
            new CurrentUser("kc-alice", "rm1", "Alice Martin", "rm1@example.com", List.of("RM"));

    private AppUserRepository users;
    private LoginEventRepository loginEvents;
    private AuditService audit;
    private SessionService service;

    @BeforeEach
    void setUp() {
        users = mock(AppUserRepository.class);
        loginEvents = mock(LoginEventRepository.class);
        audit = mock(AuditService.class);
        service = new SessionService(users, loginEvents, audit, Clock.fixed(NOW, ZoneOffset.UTC));
        when(users.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void firstSignInCreatesTheUserAndReportsFirstLogin() {
        when(users.findByKeycloakId("kc-alice")).thenReturn(Optional.empty());

        SessionStartResponse response = service.startSession(ALICE, "10.0.0.5", "Firefox");

        assertThat(response.firstLogin()).isTrue();
        assertThat(response.previousLoginAt()).isNull();
        verify(loginEvents).save(any(LoginEvent.class));
        verify(audit).record(eq("rm1"), eq("LOGIN"), any());
    }

    @Test
    void laterSignInReturnsThePreviousSignInTime() {
        Instant lastTime = Instant.parse("2026-10-18T17:30:00Z");
        AppUser existing = new AppUser("kc-alice", "rm1", "Alice Martin", "rm1@example.com", lastTime);
        existing.recordLogin(lastTime);
        when(users.findByKeycloakId("kc-alice")).thenReturn(Optional.of(existing));

        SessionStartResponse response = service.startSession(ALICE, "10.0.0.5", "Firefox");

        assertThat(response.firstLogin()).isFalse();
        assertThat(response.previousLoginAt()).isEqualTo(lastTime);
        assertThat(existing.getLastLoginAt()).isEqualTo(NOW);
    }
}
