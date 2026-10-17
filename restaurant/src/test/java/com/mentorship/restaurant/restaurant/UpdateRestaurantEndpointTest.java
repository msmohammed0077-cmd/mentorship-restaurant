package com.mentorship.restaurant.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.RestaurantEndpointTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class UpdateRestaurantEndpointTest extends RestaurantEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "update.restaurant.test.";
  }

  @Test
  void ownerUpdatesNameAndDescription() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    update(
            asRestaurantOwner(restaurantId),
            restaurantId,
            """
            { "name": "Koshary Palace", "description": "Koshary, and more." }
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
        .isEqualTo(email("koshary"))
        .jsonPath("$.is_open")
        .isEqualTo(true);

    assertThat(storedRow(restaurantId))
        .containsEntry("user_name", "Koshary Palace")
        .containsEntry("restaurant_name", "Koshary Palace")
        .containsEntry("restaurant_description", "Koshary, and more.");
  }

  /** Keycloak owns the login email (ADR 0010): one sent here is ignored, not refused. */
  @Test
  void ignoresAnEmail() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    update(
            asRestaurantOwner(restaurantId),
            restaurantId,
            """
            { "email": "%s" }
            """
                .formatted(email("palace")))
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.email")
        .isEqualTo(email("koshary"));

    assertThat(storedRow(restaurantId)).containsEntry("user_email", email("koshary"));
  }

  @Test
  void leavesAbsentAndNullFieldsUnchanged() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    update(
            asRestaurantOwner(restaurantId),
            restaurantId,
            """
            { "name": null }
            """)
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.name")
        .isEqualTo("Koshary Corner")
        .jsonPath("$.description")
        .isEqualTo("Test kitchen.");
  }

  @Test
  void trimsTheName() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    update(asRestaurantOwner(restaurantId), restaurantId, nameBody("  Koshary Palace  "))
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.name")
        .isEqualTo("Koshary Palace");

    assertThat(storedRow(restaurantId))
        .containsEntry("user_name", "Koshary Palace")
        .containsEntry("restaurant_name", "Koshary Palace");
  }

  @Test
  void refusesARequestWithoutAToken() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    update(client, restaurantId, nameBody("Koshary Palace")).expectStatus().isUnauthorized();

    assertThat(storedRow(restaurantId)).containsEntry("restaurant_name", "Koshary Corner");
  }

  @Test
  void refusesACustomer() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    update(asRole("CUSTOMER"), restaurantId, nameBody("Koshary Palace"))
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Access denied");

    assertThat(storedRow(restaurantId)).containsEntry("restaurant_name", "Koshary Corner");
  }

  @Test
  void refusesAnotherRestaurantsOwner() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long otherId = insertRestaurant("Other Corner", email("other"), true);

    refusesNotTheOwner(asRestaurantOwner(otherId), restaurantId);
  }

  /** The admin holds restaurant.write but owns no restaurant: nothing bypasses ownership. */
  @Test
  void refusesTheAdmin() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    refusesNotTheOwner(asAdmin(), restaurantId);
  }

  /** Ownership is checked before existence, so an unknown id answers as another's does. */
  @Test
  void refusesAnUnknownRestaurant() {
    long ownId = insertRestaurant("Koshary Corner", email("koshary"), true);

    update(asRestaurantOwner(ownId), 999_999L, nameBody("Koshary Palace"))
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant belongs to another owner");
  }

  @Test
  void rejectsItsOwnSoftDeletedRestaurant() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    softDeleteRestaurant(restaurantId);

    update(asRestaurantOwner(restaurantId), restaurantId, nameBody("Koshary Palace"))
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant not found");
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
  void rejectsADescriptionOver1000Characters() {
    rejectsBody(
        """
        { "description": "%s" }
        """
            .formatted("x".repeat(1001)),
        "description");
  }

  private void refusesNotTheOwner(RestTestClient caller, long restaurantId) {
    update(caller, restaurantId, nameBody("Koshary Palace"))
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant belongs to another owner");

    assertThat(storedRow(restaurantId)).containsEntry("restaurant_name", "Koshary Corner");
  }

  private RestTestClient.ResponseSpec update(
      RestTestClient caller, long restaurantId, String body) {
    return caller
        .patch()
        .uri("/api/v1/restaurants/{id}", restaurantId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .exchange();
  }

  private void rejectsBody(String body, String field) {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    update(asRestaurantOwner(restaurantId), restaurantId, body)
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
