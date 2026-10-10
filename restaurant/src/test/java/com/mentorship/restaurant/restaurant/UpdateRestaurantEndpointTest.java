package com.mentorship.restaurant.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.RestaurantEndpointTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class UpdateRestaurantEndpointTest extends RestaurantEndpointTestSupport {

  private static final String SEEDED_RESTAURANT_EMAIL = "contact@nilekitchen.example.com";
  private static final String SEEDED_CUSTOMER_EMAIL = "ahmed.ali@example.com";

  @Override
  protected String emailPrefix() {
    return "update.restaurant.test.";
  }

  @Test
  void adminUpdatesNameDescriptionAndEmail() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    update(
            restaurantId,
            "role=ADMIN",
            """
            {
              "name": "Koshary Palace",
              "description": "Koshary, and more.",
              "email": "Update.Restaurant.Test.PALACE@Example.com"
            }
            """)
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.restaurant_id")
        .isEqualTo(restaurantId)
        .jsonPath("$.name")
        .isEqualTo("Koshary Palace")
        .jsonPath("$.description")
        .isEqualTo("Koshary, and more.")
        .jsonPath("$.email")
        .isEqualTo(email("palace"))
        .jsonPath("$.is_open")
        .isEqualTo(true);

    assertThat(storedRow(restaurantId))
        .containsEntry("user_name", "Koshary Palace")
        .containsEntry("restaurant_name", "Koshary Palace")
        .containsEntry("restaurant_description", "Koshary, and more.")
        .containsEntry("user_email", email("palace"));
  }

  @Test
  void restaurantUpdatesItself() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    update(
            restaurantId,
            "role=RESTAURANT&restaurantId=" + restaurantId,
            """
            { "name": "Koshary Palace" }
            """)
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.name")
        .isEqualTo("Koshary Palace");
  }

  @Test
  void leavesAbsentAndNullFieldsUnchanged() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    update(
            restaurantId,
            "role=ADMIN",
            """
            { "name": null }
            """)
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.name")
        .isEqualTo("Koshary Corner")
        .jsonPath("$.description")
        .isEqualTo("Test kitchen.")
        .jsonPath("$.email")
        .isEqualTo(email("koshary"));
  }

  @Test
  void acceptsItsOwnEmailInAnyCase() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    update(restaurantId, "role=ADMIN", emailBody(email("koshary").toUpperCase()))
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.email")
        .isEqualTo(email("koshary"));
  }

  @Test
  void rejectsAnotherRestaurantsEmailInAnyCase() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    update(restaurantId, "role=ADMIN", emailBody(SEEDED_RESTAURANT_EMAIL.toUpperCase()))
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Email is already in use");

    assertThat(storedRow(restaurantId)).containsEntry("user_email", email("koshary"));
  }

  @Test
  void rejectsACustomersEmail() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    update(restaurantId, "role=ADMIN", emailBody(SEEDED_CUSTOMER_EMAIL))
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Email is already in use");
  }

  @ParameterizedTest
  @ValueSource(strings = {"CUSTOMER", "COURIER", "SYSTEM"})
  void rejectsEveryRoleButAdminAndRestaurant(String role) {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    update(restaurantId, "role=" + role, nameBody("Koshary Palace"))
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Role " + role + " may not edit this restaurant");

    assertThat(storedRow(restaurantId)).containsEntry("restaurant_name", "Koshary Corner");
  }

  @Test
  void rejectsARestaurantEditingAnother() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long otherId = insertRestaurant("Other Corner", email("other"), true);

    update(restaurantId, "role=RESTAURANT&restaurantId=" + otherId, nameBody("Koshary Palace"))
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Role RESTAURANT may not edit this restaurant");
  }

  @Test
  void rejectsARestaurantWithoutItsId() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    update(restaurantId, "role=RESTAURANT", nameBody("Koshary Palace"))
        .expectStatus()
        .isForbidden();
  }

  @Test
  void checksTheRoleBeforeExistence() {
    update(999_999L, "role=CUSTOMER", nameBody("Koshary Palace")).expectStatus().isForbidden();
  }

  @Test
  void rejectsAnUnknownRestaurant() {
    update(999_999L, "role=ADMIN", nameBody("Koshary Palace"))
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant not found");
  }

  @Test
  void rejectsASoftDeletedRestaurant() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    softDeleteRestaurant(restaurantId);

    update(restaurantId, "role=ADMIN", nameBody("Koshary Palace")).expectStatus().isNotFound();
  }

  @Test
  void rejectsAMissingRole() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    client
        .patch()
        .uri("/api/v1/restaurants/{id}", restaurantId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(nameBody("Koshary Palace"))
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("role is required");
  }

  @Test
  void rejectsABlankName() {
    rejectsBody(nameBody("   "), "name");
  }

  @Test
  void rejectsANameOver150Characters() {
    rejectsBody(nameBody("x".repeat(151)), "name");
  }

  @Test
  void rejectsABlankEmail() {
    rejectsBody(emailBody(""), "email");
  }

  /** {@code @Email} refuses it before the service runs, so the service does not trim. */
  @Test
  void rejectsAnEmailWithSurroundingWhitespace() {
    rejectsBody(emailBody(" " + email("koshary") + " "), "email");
  }

  @Test
  void rejectsAMalformedEmail() {
    rejectsBody(emailBody("not-an-email"), "email");
  }

  private RestTestClient.ResponseSpec update(long restaurantId, String query, String body) {
    return client
        .patch()
        .uri("/api/v1/restaurants/{id}?" + query, restaurantId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .exchange();
  }

  private void rejectsBody(String body, String field) {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    update(restaurantId, "role=ADMIN", body)
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .value(String.class, message -> assertThat(message).contains(field));
  }

  private static String nameBody(String name) {
    return """
        { "name": "%s" }
        """
        .formatted(name);
  }

  private static String emailBody(String email) {
    return """
        { "email": "%s" }
        """
        .formatted(email);
  }

  private Map<String, Object> storedRow(long restaurantId) {
    return jdbcTemplate.queryForMap(
        """
        SELECT u.user_name, u.user_email, r.restaurant_name, r.restaurant_description
        FROM restaurants r JOIN users u ON u.user_id = r.user_id
        WHERE r.restaurant_id = ?
        """,
        restaurantId);
  }
}
