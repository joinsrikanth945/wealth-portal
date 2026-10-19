package com.yourbank.wealth.book;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yourbank.wealth.book.BookDtos.RmHome;
import com.yourbank.wealth.shared.security.KeycloakRealmRoleConverter;
import com.yourbank.wealth.shared.security.SecurityConfig;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(BookController.class)
@Import(SecurityConfig.class)
class BookControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BookService book;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor as(String username, String role) {
        return jwt()
                .jwt(j -> j.subject("kc-" + username)
                        .claim("preferred_username", username)
                        .claim("realm_access", Map.of("roles", List.of(role))))
                .authorities(new KeycloakRealmRoleConverter());
    }

    @Test
    void rmGetsTheHomepageCounters() throws Exception {
        when(book.home(any())).thenReturn(new RmHome(new BigDecimal("1250000.00"), 7, 8, 1, 2, 1, 1, 1, 1, List.of()));

        mvc.perform(get("/api/rm/home").with(as("rm1", "RM")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clients").value(7))
                .andExpect(jsonPath("$.mifidMissingOrExpired").value(2));
    }

    @Test
    void clientsCannotListTheRmBook() throws Exception {
        mvc.perform(get("/api/rm/clients").with(as("client1", "CLIENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rmsCannotUseTheClientSelfService() throws Exception {
        mvc.perform(get("/api/client/me").with(as("rm1", "RM")))
                .andExpect(status().isForbidden());
    }

    @Test
    void noTokenMeansUnauthorized() throws Exception {
        mvc.perform(get("/api/rm/clients")).andExpect(status().isUnauthorized());
    }
}
