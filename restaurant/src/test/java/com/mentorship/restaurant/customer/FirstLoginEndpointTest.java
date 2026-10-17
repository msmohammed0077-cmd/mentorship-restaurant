package com.mentorship.restaurant.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.CustomerEndpointTestSupport;
import com.mentorship.restaurant.support.RealmPermissions;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Sign-up happens on Keycloak's page; our users and customers rows appear on the new user's first
 * request (#83 part 3, ADR 0010). Only a verified email with the customer permissions qualifies.
 */
class FirstLoginEndpointTest extends CustomerEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "first.login.test.";
  }

  @Test
  void createsTheUserAndCustomerOnTheFirstRequest() {
    UUID keycloakId = UUID.randomUUID();

    getMe(jwts.signUpTokenFor(keycloakId, "  First.Login.Test.Nour@Example.com ", "Nour Adel"))
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.name")
        .isEqualTo("Nour Adel")
        .jsonPath("$.email")
        .isEqualTo(email("nour"))
        .jsonPath("$.phone")
        .doesNotExist();

    assertThat(usersLinkedTo(keycloakId)).isEqualTo(1);
    assertThat(customersLinkedTo(keycloakId)).isEqualTo(1);
  }

  @Test
  void reusesTheRowsOnTheNextRequest() {
    UUID keycloakId = UUID.randomUUID();
    String token = jwts.signUpTokenFor(keycloakId, email("nour"), "Nour Adel");

    Integer first = customerIdFrom(getMe(token));
    Integer second = customerIdFrom(getMe(token));

    assertThat(second).isEqualTo(first);
    assertThat(usersLinkedTo(keycloakId)).isEqualTo(1);
  }

  @Test
  void namesTheCustomerAfterTheirEmailWhenTheTokenHasNoName() {
    UUID keycloakId = UUID.randomUUID();
    JwtClaimsSet.Builder claims =
        jwts.signUpClaims(keycloakId, email("nour"), "unused").claims(all -> all.remove("name"));

    getMe(jwts.sign(claims))
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.name")
        .isEqualTo(email("nour"));
  }

  @Test
  void cutsANameLongerThanTheColumn() {
    UUID keycloakId = UUID.randomUUID();

    getMe(jwts.signUpTokenFor(keycloakId, email("nour"), "N".repeat(151)))
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.name")
        .isEqualTo("N".repeat(150));
  }

  @Test
  void refusesATokenWithoutTheCustomerPermissions() {
    UUID keycloakId = UUID.randomUUID();
    JwtClaimsSet.Builder claims =
        jwts.signUpClaims(keycloakId, email("nour"), "Nour Adel")
            .claim(
                "resource_access",
                Map.of(
                    "restaurant-api",
                    Map.of("roles", List.copyOf(RealmPermissions.of("RESTAURANT_OWNER")))));

    getMe(jwts.sign(claims)).expectStatus().isUnauthorized();

    assertThat(usersLinkedTo(keycloakId)).isZero();
  }

  @Test
  void keepsACharacterWholeWhenCuttingTheName() {
    UUID keycloakId = UUID.randomUUID();
    String name = "a".repeat(149) + "\uD83D\uDE00" + "b";

    getMe(jwts.signUpTokenFor(keycloakId, email("nour"), name))
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.name")
        .isEqualTo("a".repeat(149) + "\uD83D\uDE00");
  }

  /** Staff are linked to their rows by hand; the default role must not make them customers. */
  @Test
  void refusesAKitchenToken() {
    UUID keycloakId = UUID.randomUUID();
    Set<String> ownerAndCustomer = new TreeSet<>(RealmPermissions.of("CUSTOMER"));
    ownerAndCustomer.addAll(RealmPermissions.of("RESTAURANT_OWNER"));

    getMe(jwts.sign(withPermissions(keycloakId, ownerAndCustomer))).expectStatus().isUnauthorized();

    assertThat(usersLinkedTo(keycloakId)).isZero();
  }

  @Test
  void refusesAnAdminToken() {
    UUID keycloakId = UUID.randomUUID();

    getMe(jwts.sign(withPermissions(keycloakId, RealmPermissions.of("ADMIN"))))
        .expectStatus()
        .isUnauthorized();

    assertThat(usersLinkedTo(keycloakId)).isZero();
  }

  @Test
  void refusesATokenIssuedToAnotherClient() {
    UUID keycloakId = UUID.randomUUID();

    getMe(
            jwts.sign(
                jwts.signUpClaims(keycloakId, email("nour"), "Nour Adel")
                    .claim("azp", "some-other-client")))
        .expectStatus()
        .isUnauthorized();

    assertThat(usersLinkedTo(keycloakId)).isZero();
  }

  @Test
  void refusesATokenWithoutAnEmail() {
    UUID keycloakId = UUID.randomUUID();
    JwtClaimsSet.Builder claims =
        jwts.signUpClaims(keycloakId, email("nour"), "Nour Adel")
            .claims(all -> all.remove("email"));

    getMe(jwts.sign(claims)).expectStatus().isUnauthorized();

    assertThat(usersLinkedTo(keycloakId)).isZero();
  }

  @Test
  void refusesAnUnverifiedEmail() {
    UUID keycloakId = UUID.randomUUID();
    JwtClaimsSet.Builder claims =
        jwts.signUpClaims(keycloakId, email("nour"), "Nour Adel").claim("email_verified", false);

    getMe(jwts.sign(claims)).expectStatus().isUnauthorized();

    assertThat(usersLinkedTo(keycloakId)).isZero();
  }

  @Test
  void refusesAnEmailHeldByAnUnlinkedAccount() {
    long existing = insertCustomerWithoutKeycloakId(email("nour"));
    UUID keycloakId = UUID.randomUUID();

    getMe(jwts.signUpTokenFor(keycloakId, email("nour"), "Nour Adel"))
        .expectStatus()
        .isUnauthorized()
        .expectHeader()
        .valueMatches(HttpHeaders.WWW_AUTHENTICATE, ".*not linked to Keycloak.*");

    assertThat(usersLinkedTo(keycloakId)).isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT u.user_keycloak_id IS NULL FROM users u
                JOIN customers c ON c.user_id = u.user_id WHERE c.customer_id = ?
                """,
                Boolean.class,
                existing))
        .isTrue();
  }

  @Test
  void reusesTheEmailOfASoftDeletedAccount() {
    long deleted = insertCustomer(email("nour"));
    softDeleteCustomer(deleted);
    UUID keycloakId = UUID.randomUUID();

    Integer created = customerIdFrom(getMe(jwts.signUpTokenFor(keycloakId, email("nour"), "Nour")));

    assertThat(created.longValue()).isNotEqualTo(deleted);
    assertThat(customersLinkedTo(keycloakId)).isEqualTo(1);
  }

  private JwtClaimsSet.Builder withPermissions(UUID keycloakId, Set<String> permissions) {
    return jwts.signUpClaims(keycloakId, email("nour"), "Nour Adel")
        .claim(
            "resource_access", Map.of("restaurant-api", Map.of("roles", List.copyOf(permissions))));
  }

  private RestTestClient.ResponseSpec getMe(String token) {
    return client
        .get()
        .uri("/api/v1/customers/me")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .exchange();
  }

  private static Integer customerIdFrom(RestTestClient.ResponseSpec response) {
    Map<?, ?> body =
        response.expectStatus().isOk().expectBody(Map.class).returnResult().getResponseBody();
    return (Integer) body.get("customer_id");
  }

  private int usersLinkedTo(UUID keycloakId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE user_keycloak_id = ?", Integer.class, keycloakId);
    return count == null ? 0 : count;
  }

  private int customersLinkedTo(UUID keycloakId) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM customers c JOIN users u ON u.user_id = c.user_id
            WHERE u.user_keycloak_id = ?
            """,
            Integer.class,
            keycloakId);
    return count == null ? 0 : count;
  }
}
