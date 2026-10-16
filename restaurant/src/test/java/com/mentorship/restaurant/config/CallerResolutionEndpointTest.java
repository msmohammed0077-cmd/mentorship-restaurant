package com.mentorship.restaurant.config;

import com.mentorship.restaurant.support.JwtTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * The caller behind a token (#83 parts 2 and 3, ADR 0009): an unknown {@code sub} is refused unless
 * it is a customer's first request (FirstLoginEndpointTest); a linked one resolves, and a
 * non-customer or deleted customer is answered by the customer endpoints' own rules.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class CallerResolutionEndpointTest {

  private static final long NONE = 999_999L;

  @Autowired private RestTestClient client;
  @Autowired private JwtTestSupport jwts;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void refusesAnUnknownUserWhoIsNotASignUp() {
    client
        .get()
        .uri("/api/v1/orders/{orderId}", NONE)
        .header(
            HttpHeaders.AUTHORIZATION,
            "Bearer " + jwts.tokenFor(UUID.randomUUID(), "RESTAURANT_OWNER"))
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .valueMatches(HttpHeaders.WWW_AUTHENTICATE, "Bearer.*");
  }

  @Test
  void resolvesTheSeededAdmin() {
    client
        .get()
        .uri("/api/v1/orders/{orderId}", NONE)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwts.tokenFor(JwtTestSupport.ADMIN, "ADMIN"))
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void refusesANonCustomerOnACustomerEndpoint() {
    client
        .get()
        .uri("/api/v1/customers/me")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwts.tokenFor(JwtTestSupport.ADMIN, "ADMIN"))
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Caller is not a customer");
  }

  @Test
  void answersASoftDeletedCustomersOwnTokenWithNotFound() {
    UUID keycloakId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        WITH new_user AS (
          INSERT INTO users (user_name, user_email, user_keycloak_id, user_deleted_at)
          VALUES ('Gone', 'caller.resolution.test.gone@example.com', ?, now())
          RETURNING user_id
        )
        INSERT INTO customers (user_id) SELECT user_id FROM new_user
        """,
        keycloakId);
    try {
      client
          .get()
          .uri("/api/v1/customers/me")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwts.tokenFor(keycloakId, "CUSTOMER"))
          .exchange()
          .expectStatus()
          .isNotFound();
    } finally {
      jdbcTemplate.update(
          "DELETE FROM users WHERE user_email = 'caller.resolution.test.gone@example.com'");
    }
  }
}
