package com.yourbank.wealth.shared;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI with a working "Authorize" button: it signs in through Keycloak
 * (authorization code + PKCE), then sends the token with every "Try it out".
 */
@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI wealthApi(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer) {
        OAuthFlow flow = new OAuthFlow()
                .authorizationUrl(issuer + "/protocol/openid-connect/auth")
                .tokenUrl(issuer + "/protocol/openid-connect/token")
                .scopes(new Scopes().addString("openid", "Sign in"));

        return new OpenAPI()
                .info(new Info()
                        .title("Wealth Portal API")
                        .version("0.2")
                        .description("Click Authorize, sign in as rm1 / client1 / admin1, then use Try it out."))
                .components(new Components().addSecuritySchemes("keycloak", new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .flows(new OAuthFlows().authorizationCode(flow))))
                .addSecurityItem(new SecurityRequirement().addList("keycloak"));
    }
}
