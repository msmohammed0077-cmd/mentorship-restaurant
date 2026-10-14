package com.mentorship.restaurant.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI's Authorize button: Keycloak's login page, authorization code with PKCE, client
 * {@code restaurant-swagger} (set in application.properties). Endpoints are derived from the
 * issuer the API trusts, so the two cannot drift apart.
 */
@Configuration
public class OpenApiConfig {

  private static final String KEYCLOAK = "keycloak";

  @Bean
  OpenAPI openApi(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer) {
    String oidc = issuer + "/protocol/openid-connect";
    OAuthFlow authorizationCode =
        new OAuthFlow()
            .authorizationUrl(oidc + "/auth")
            .tokenUrl(oidc + "/token")
            .scopes(new Scopes().addString("openid", "OpenID Connect"));
    return new OpenAPI()
        .components(
            new Components()
                .addSecuritySchemes(
                    KEYCLOAK,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.OAUTH2)
                        .flows(new OAuthFlows().authorizationCode(authorizationCode))))
        .addSecurityItem(new SecurityRequirement().addList(KEYCLOAK));
  }
}
