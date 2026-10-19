package com.yourbank.wealth.identity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.security.oauth2.jwt.Jwt;

/** The signed-in user, read from the Keycloak access token. */
public record CurrentUser(
        String keycloakId,
        String username,
        String displayName,
        String email,
        List<String> roles) {

    public static CurrentUser from(Jwt jwt) {
        String username = Objects.requireNonNullElse(jwt.getClaimAsString("preferred_username"), jwt.getSubject());
        String given = Objects.requireNonNullElse(jwt.getClaimAsString("given_name"), "");
        String family = Objects.requireNonNullElse(jwt.getClaimAsString("family_name"), "");
        String displayName = (given + " " + family).trim();
        return new CurrentUser(
                jwt.getSubject(),
                username,
                displayName.isEmpty() ? username : displayName,
                jwt.getClaimAsString("email"),
                realmRoles(jwt));
    }

    public boolean isAdmin() {
        return roles.contains("ADMIN");
    }

    public boolean isRm() {
        return roles.contains("RM");
    }

    public boolean isClient() {
        return roles.contains("CLIENT");
    }

    private static List<String> realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
            return roles.stream()
                    .map(String::valueOf)
                    .filter(r -> r.equals("CLIENT") || r.equals("RM") || r.equals("PM") || r.equals("ADMIN"))
                    .toList();
        }
        return List.of();
    }
}
