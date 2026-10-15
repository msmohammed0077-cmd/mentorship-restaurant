package com.mentorship.restaurant.config;

import com.mentorship.restaurant.support.JwtTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * The caller behind a token (#83 part 2, ADR 0009): a token whose {@code sub} has no {@code users}
 * row is refused; a linked one resolves.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class CallerResolutionEndpointTest {

  private static final long NONE = 999_999L;

  @Autowired private RestTestClient client;
  @Autowired private JwtTestSupport jwts;

  @Test
  void refusesATokenWhoseUserHasNoAccount() {
    client
        .get()
        .uri("/api/v1/orders/{orderId}", NONE)
        .header(
            HttpHeaders.AUTHORIZATION, "Bearer " + jwts.tokenFor(UUID.randomUUID(), "CUSTOMER"))
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
}
