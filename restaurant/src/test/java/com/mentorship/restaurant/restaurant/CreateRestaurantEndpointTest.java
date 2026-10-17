package com.mentorship.restaurant.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.JwtTestSupport;
import com.mentorship.restaurant.support.RestaurantEndpointTestSupport;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class CreateRestaurantEndpointTest extends RestaurantEndpointTestSupport {

  private static final String SEEDED_RESTAURANT_EMAIL = "contact@nilekitchen.example.com";
  private static final String SEEDED_CUSTOMER_EMAIL = "ahmed.ali@example.com";
  private static final UUID SEEDED_CUSTOMER_KEYCLOAK_ID =
      UUID.fromString("00000000-0000-4000-8000-000000000001");

  @Override
  protected String emailPrefix() {
    return "create.restaurant.test.";
  }

  @Test
  void createsAClosedRestaurantLinkedToItsOwner() {
    UUID owner = UUID.randomUUID();

    asAdmin()
        .post()
        .uri("/api/v1/restaurants")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {
              "name": "Koshary Corner",
              "description": "Koshary, the Cairo way.",
              "email": "Create.Restaurant.Test.KOSHARY@Example.com",
              "owner_keycloak_id": "%s",
              "is_open": true
            }
            """
                .formatted(owner))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.restaurant_id")
        .isNotEmpty()
        .jsonPath("$.name")
        .isEqualTo("Koshary Corner")
        .jsonPath("$.description")
        .isEqualTo("Koshary, the Cairo way.")
        .jsonPath("$.email")
        .isEqualTo(email("koshary"))
        .jsonPath("$.is_open")
        .isEqualTo(false)
        .jsonPath("$.owner_keycloak_id")
        .doesNotExist();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            """
            SELECT u.user_name, u.user_keycloak_id, r.restaurant_name, r.restaurant_is_open
            FROM restaurants r JOIN users u ON u.user_id = r.user_id
            WHERE u.user_email = ?
            """,
            email("koshary"));
    assertThat(row)
        .containsEntry("user_name", "Koshary Corner")
        .containsEntry("restaurant_name", "Koshary Corner")
        .containsEntry("user_keycloak_id", owner)
        .containsEntry("restaurant_is_open", false);
  }

  @Test
  void trimsTheName() {
    asAdmin()
        .post()
        .uri("/api/v1/restaurants")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body("  Koshary Corner  ", email("koshary")))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.name")
        .isEqualTo("Koshary Corner");

    assertThat(
            jdbcTemplate.queryForMap(
                """
                SELECT u.user_name, r.restaurant_name
                FROM restaurants r JOIN users u ON u.user_id = r.user_id
                WHERE u.user_email = ?
                """,
                email("koshary")))
        .containsEntry("user_name", "Koshary Corner")
        .containsEntry("restaurant_name", "Koshary Corner");
  }

  @Test
  void createsARestaurantWithoutADescription() {
    asAdmin()
        .post()
        .uri("/api/v1/restaurants")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body("Koshary Corner", email("koshary")))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.description")
        .doesNotExist();
  }

  @Test
  void refusesARequestWithoutAToken() {
    client
        .post()
        .uri("/api/v1/restaurants")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body("Koshary Corner", email("koshary")))
        .exchange()
        .expectStatus()
        .isUnauthorized();

    assertThat(usersWithEmail(email("koshary"))).isZero();
  }

  @ParameterizedTest
  @ValueSource(strings = {"CUSTOMER", "RESTAURANT_OWNER"})
  void refusesEveryRoleButAdmin(String realmRole) {
    create(asRole(realmRole), body("Koshary Corner", email("koshary")))
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Access denied");

    assertThat(usersWithEmail(email("koshary"))).isZero();
  }

  @Test
  void checksThePermissionBeforeTheEmail() {
    create(asRole("RESTAURANT_OWNER"), body("Koshary Corner", SEEDED_RESTAURANT_EMAIL))
        .expectStatus()
        .isForbidden();
  }

  @Test
  void rejectsABlankName() {
    rejectsBody(body("   ", email("koshary")), "name");
  }

  @Test
  void rejectsANameOver150Characters() {
    rejectsBody(body("x".repeat(151), email("koshary")), "name");
  }

  @Test
  void rejectsAMissingEmail() {
    rejectsBody(
        """
        { "name": "Koshary Corner", "owner_keycloak_id": "%s" }
        """
            .formatted(UUID.randomUUID()),
        "email");
  }

  @Test
  void rejectsAMalformedEmail() {
    rejectsBody(body("Koshary Corner", "not-an-email"), "email");
  }

  @Test
  void rejectsAMissingOwnerKeycloakId() {
    rejectsBody(
        """
        { "name": "Koshary Corner", "email": "%s" }
        """
            .formatted(email("koshary")),
        "ownerKeycloakId");
  }

  @Test
  void rejectsAMalformedOwnerKeycloakId() {
    create(
            asAdmin(),
            """
            { "name": "Koshary Corner", "email": "%s", "owner_keycloak_id": "not-a-uuid" }
            """
                .formatted(email("koshary")))
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Request body is malformed");
  }

  @Test
  void rejectsADescriptionOver1000Characters() {
    rejectsBody(
        """
        { "name": "Koshary Corner", "email": "%s", "owner_keycloak_id": "%s", "description": "%s" }
        """
            .formatted(email("koshary"), UUID.randomUUID(), "x".repeat(1001)),
        "description");
  }

  @Test
  void acceptsADescriptionOf1000Characters() {
    create(
            asAdmin(),
            """
            { "name": "Koshary Corner", "email": "%s", "owner_keycloak_id": "%s", "description": "%s" }
            """
                .formatted(email("koshary"), UUID.randomUUID(), "x".repeat(1000)))
        .expectStatus()
        .isCreated();
  }

  /** {@code @Email} refuses it before the service runs, so the service does not trim. */
  @Test
  void rejectsAnEmailWithSurroundingWhitespace() {
    rejectsBody(body("Koshary Corner", " " + email("koshary") + " "), "email");
  }

  /**
   * 256 characters that are otherwise a valid email, so only {@code @Size} refuses it:
   * {@code @Email} caps the local part at 64 and each domain label at 63.
   */
  @Test
  void rejectsAnEmailOver255Characters() {
    String local = emailPrefix() + "x".repeat(60 - emailPrefix().length());
    String domain = "a".repeat(63) + "." + "b".repeat(63) + "." + "c".repeat(63) + ".com";
    String email = local + "@" + domain;
    assertThat(email).hasSize(256);

    rejectsBody(body("Koshary Corner", email), "email");
  }

  @Test
  void rejectsARestaurantsEmailInAnyCase() {
    create(asAdmin(), body("Koshary Corner", SEEDED_RESTAURANT_EMAIL.toUpperCase()))
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Email is already in use");
  }

  @Test
  void rejectsACustomersEmail() {
    create(asAdmin(), body("Koshary Corner", SEEDED_CUSTOMER_EMAIL))
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Email is already in use");
  }

  @Test
  void allowsReusingTheEmailOfASoftDeletedRestaurant() {
    softDeleteRestaurant(insertRestaurant("Old Corner", email("koshary"), true));

    create(asAdmin(), body("Koshary Corner", email("koshary"))).expectStatus().isCreated();
  }

  @Test
  void rejectsAnOwnerAlreadyLinkedToACustomer() {
    rejectsLinkedOwner(SEEDED_CUSTOMER_KEYCLOAK_ID);
  }

  @Test
  void rejectsAnOwnerAlreadyLinkedToTheAdmin() {
    rejectsLinkedOwner(JwtTestSupport.ADMIN);
  }

  /** The unique constraint covers deleted rows too, so a deleted account's id stays taken. */
  @Test
  void rejectsTheOwnerOfASoftDeletedRestaurant() {
    long oldId = insertRestaurant("Old Corner", email("old"), true);
    softDeleteRestaurant(oldId);

    rejectsLinkedOwner(ownerKeycloakIdOf(oldId));
  }

  private void rejectsLinkedOwner(UUID owner) {
    create(asAdmin(), body("Koshary Corner", email("koshary"), owner))
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Owner account is already linked");

    assertThat(usersWithEmail(email("koshary"))).isZero();
  }

  private RestTestClient.ResponseSpec create(RestTestClient caller, String body) {
    return caller
        .post()
        .uri("/api/v1/restaurants")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .exchange();
  }

  private void rejectsBody(String body, String field) {
    create(asAdmin(), body)
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .value(String.class, message -> assertThat(message).contains(field));
  }

  private static String body(String name, String email) {
    return body(name, email, UUID.randomUUID());
  }

  private static String body(String name, String email, UUID owner) {
    return """
        { "name": "%s", "email": "%s", "owner_keycloak_id": "%s" }
        """
        .formatted(name, email, owner);
  }

  private int usersWithEmail(String email) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM users WHERE user_email = ?", Integer.class, email);
    return count == null ? 0 : count;
  }
}
