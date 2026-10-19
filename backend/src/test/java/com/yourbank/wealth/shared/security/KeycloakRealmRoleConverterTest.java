package com.yourbank.wealth.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    @Test
    void mapsAppRolesAndIgnoresKeycloakDefaults() {
        Jwt jwt = jwtWithClaims(Map.of(
                "realm_access", Map.of("roles", List.of("RM", "offline_access", "uma_authorization"))));

        assertThat(converter.convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_RM");
    }

    @Test
    void returnsNoAuthoritiesWhenClaimIsMissing() {
        Jwt jwt = jwtWithClaims(Map.of("preferred_username", "someone"));

        assertThat(converter.convert(jwt)).isEmpty();
    }

    static Jwt jwtWithClaims(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("kc-123")
                .issuedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .expiresAt(Instant.parse("2026-01-01T01:00:00Z"));
        claims.forEach(builder::claim);
        return builder.build();
    }
}
