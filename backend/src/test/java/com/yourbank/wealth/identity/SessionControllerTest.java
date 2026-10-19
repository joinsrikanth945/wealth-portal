package com.yourbank.wealth.identity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yourbank.wealth.identity.SessionResponses.MeResponse;
import com.yourbank.wealth.identity.SessionResponses.SessionStartResponse;
import com.yourbank.wealth.shared.security.SecurityConfig;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SessionController.class)
@Import(SecurityConfig.class)
class SessionControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SessionService sessions;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void rejectsRequestsWithoutAToken() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void returnsTheSignedInUser() throws Exception {
        when(sessions.me(any())).thenReturn(new MeResponse(
                "rm1", "Alice Martin", "rm1@example.com", List.of("RM"), Instant.parse("2026-10-18T17:30:00Z")));

        mvc.perform(get("/api/me").with(jwt().jwt(j -> j
                        .subject("kc-alice")
                        .claim("preferred_username", "rm1")
                        .claim("realm_access", Map.of("roles", List.of("RM"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("rm1"))
                .andExpect(jsonPath("$.roles[0]").value("RM"));
    }

    @Test
    void startsASession() throws Exception {
        when(sessions.startSession(any(), any(), any())).thenReturn(new SessionStartResponse(null, true));

        mvc.perform(post("/api/session/start").with(jwt().jwt(j -> j.subject("kc-alice"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstLogin").value(true));
    }

    @Test
    void keepsClientsOutOfTheRmArea() throws Exception {
        mvc.perform(get("/api/rm/anything").with(jwt().jwt(j -> j
                        .subject("kc-bob")
                        .claim("realm_access", Map.of("roles", List.of("CLIENT"))))))
                .andExpect(status().isForbidden());
    }
}
