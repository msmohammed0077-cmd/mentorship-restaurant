package com.mentorship.restaurant.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.RestaurantEndpointTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class CreateRestaurantEndpointTest extends RestaurantEndpointTestSupport {

  private static final String SEEDED_RESTAURANT_EMAIL = "contact@nilekitchen.example.com";
  private static final String SEEDED_CUSTOMER_EMAIL = "ahmed.ali@example.com";

  @Override
  protected String emailPrefix() {
    return "create.restaurant.test.";
  }

  @Test
  void createsAClosedRestaurantWithANoLoginAccount() {
    client
        .post()
        .uri("/api/v1/restaurants?role=ADMIN")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {
              "name": "Koshary Corner",
              "description": "Koshary, the Cairo way.",
              "email": "Create.Restaurant.Test.KOSHARY@Example.com",
              "is_open": true,
              "password": "ignored"
            }
            """)
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
        .jsonPath("$.password")
        .doesNotExist();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            """
            SELECT u.user_name, u.user_password, r.restaurant_name, r.restaurant_is_open
            FROM restaurants r JOIN users u ON u.user_id = r.user_id
            WHERE u.user_email = ?
            """,
            email("koshary"));
    assertThat(row)
        .containsEntry("user_name", "Koshary Corner")
        .containsEntry("restaurant_name", "Koshary Corner")
        .containsEntry("user_password", "!no-login")
        .containsEntry("restaurant_is_open", false);
  }

  @Test
  void createsARestaurantWithoutADescription() {
    client
        .post()
        .uri("/api/v1/restaurants?role=ADMIN")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            { "name": "Koshary Corner", "email": "%s" }
            """
                .formatted(email("koshary")))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.description")
        .doesNotExist();
  }

  @ParameterizedTest
  @ValueSource(strings = {"CUSTOMER", "RESTAURANT", "COURIER", "SYSTEM"})
  void rejectsEveryRoleButAdmin(String role) {
    createRestaurant(role, email("koshary"))
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Role " + role + " may not create a restaurant");

    assertThat(usersWithEmail(email("koshary"))).isZero();
  }

  @Test
  void checksTheRoleBeforeTheEmail() {
    createRestaurant("CUSTOMER", SEEDED_RESTAURANT_EMAIL).expectStatus().isForbidden();
  }

  @Test
  void rejectsAMissingRole() {
    client
        .post()
        .uri("/api/v1/restaurants")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body("Koshary Corner", email("koshary")))
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("role is required");
  }

  @Test
  void rejectsAnUnknownRole() {
    createRestaurant("OWNER", email("koshary"))
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("role is not a valid value");
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
        { "name": "Koshary Corner" }
        """,
        "email");
  }

  @Test
  void rejectsAMalformedEmail() {
    rejectsBody(body("Koshary Corner", "not-an-email"), "email");
  }

  @Test
  void rejectsAnEmailOver255Characters() {
    rejectsBody(body("Koshary Corner", emailPrefix() + "x".repeat(250) + "@example.com"), "email");
  }

  @Test
  void rejectsARestaurantsEmailInAnyCase() {
    createRestaurant("ADMIN", SEEDED_RESTAURANT_EMAIL.toUpperCase())
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Email is already in use");
  }

  @Test
  void rejectsACustomersEmail() {
    createRestaurant("ADMIN", SEEDED_CUSTOMER_EMAIL)
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Email is already in use");
  }

  @Test
  void allowsReusingTheEmailOfASoftDeletedRestaurant() {
    softDeleteRestaurant(insertRestaurant("Old Corner", email("koshary"), true));

    createRestaurant("ADMIN", email("koshary")).expectStatus().isCreated();
  }

  private RestTestClient.ResponseSpec createRestaurant(String role, String email) {
    return client
        .post()
        .uri("/api/v1/restaurants?role={role}", role)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body("Koshary Corner", email))
        .exchange();
  }

  private void rejectsBody(String body, String field) {
    client
        .post()
        .uri("/api/v1/restaurants?role=ADMIN")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .value(String.class, message -> assertThat(message).contains(field));
  }

  private static String body(String name, String email) {
    return """
        { "name": "%s", "email": "%s" }
        """
        .formatted(name, email);
  }

  private int usersWithEmail(String email) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM users WHERE user_email = ?", Integer.class, email);
    return count == null ? 0 : count;
  }
}
