package com.yourbank.wealth.identity;

import com.yourbank.wealth.identity.SessionResponses.MeResponse;
import com.yourbank.wealth.identity.SessionResponses.SessionStartResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SessionController {

    private final SessionService sessions;

    public SessionController(SessionService sessions) {
        this.sessions = sessions;
    }

    /** Records this sign-in; returns the previous sign-in time. */
    @PostMapping("/session/start")
    public SessionStartResponse start(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
        return sessions.startSession(
                CurrentUser.from(jwt), request.getRemoteAddr(), request.getHeader(HttpHeaders.USER_AGENT));
    }

    /** Who am I: profile, roles and last sign-in. */
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return sessions.me(CurrentUser.from(jwt));
    }
}
