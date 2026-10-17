package com.mentorship.restaurant.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.CustomerEndpointTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Keycloak owns the login email (ADR 0010): when it changes there, our copy follows on the next
 * request, so a stale copy can never block whoever registers with the freed address.
 */
class KeycloakEmailSyncEndpointTest extends CustomerEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "email.sync.test.";
  }

  @Test
  void followsAnEmailChangedInKeycloak() {
    long customerId = insertCustomer(email("old"));

    getMe(customerId, claimsFor(customerId, email("new")))
        .jsonPath("$.email")
        .isEqualTo(email("new"));

    assertThat(storedEmailOf(customerId)).isEqualTo(email("new"));
  }

  @Test
  void ignoresAnUnverifiedEmail() {
    long customerId = insertCustomer(email("old"));

    getMe(customerId, claimsFor(customerId, email("new")).claim("email_verified", false));

    assertThat(storedEmailOf(customerId)).isEqualTo(email("old"));
  }

  @Test
  void keepsTheStoredEmailWhenAnotherAccountHoldsTheNewOne() {
    long customerId = insertCustomer(email("old"));
    insertCustomer(email("taken"));

    getMe(customerId, claimsFor(customerId, email("taken")));

    assertThat(storedEmailOf(customerId)).isEqualTo(email("old"));
  }

  private JwtClaimsSet.Builder claimsFor(long customerId, String email) {
    return jwts.signUpClaims(keycloakIdOf(customerId), email, "Sara Youssef");
  }

  private RestTestClient.BodyContentSpec getMe(long customerId, JwtClaimsSet.Builder claims) {
    return client
        .get()
        .uri("/api/v1/customers/me")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwts.sign(claims))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody();
  }

  private UUID keycloakIdOf(long customerId) {
    return jdbcTemplate.queryForObject(
        """
        SELECT u.user_keycloak_id FROM users u
        JOIN customers c ON c.user_id = u.user_id WHERE c.customer_id = ?
        """,
        UUID.class,
        customerId);
  }

  private String storedEmailOf(long customerId) {
    return jdbcTemplate.queryForObject(
        """
        SELECT u.user_email FROM users u
        JOIN customers c ON c.user_id = u.user_id WHERE c.customer_id = ?
        """,
        String.class,
        customerId);
  }
}
