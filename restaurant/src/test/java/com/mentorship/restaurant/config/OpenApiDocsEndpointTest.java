package com.mentorship.restaurant.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;

/** Swagger UI's Authorize button logs in through Keycloak (#83). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class OpenApiDocsEndpointTest {

  private static final String OIDC =
      "http://localhost:8180/realms/restaurant/protocol/openid-connect";

  @Autowired private RestTestClient client;

  @Test
  void publishesKeycloakAuthorizationCodeFlow() {
    client
        .get()
        .uri("/v3/api-docs")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.components.securitySchemes.keycloak.type")
        .isEqualTo("oauth2")
        .jsonPath("$.components.securitySchemes.keycloak.flows.authorizationCode.authorizationUrl")
        .isEqualTo(OIDC + "/auth")
        .jsonPath("$.components.securitySchemes.keycloak.flows.authorizationCode.tokenUrl")
        .isEqualTo(OIDC + "/token")
        .jsonPath("$.security[0].keycloak")
        .exists();
  }
}
