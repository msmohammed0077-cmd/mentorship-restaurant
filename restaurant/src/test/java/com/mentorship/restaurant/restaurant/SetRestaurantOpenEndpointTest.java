package com.mentorship.restaurant.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.RestaurantEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class SetRestaurantOpenEndpointTest extends RestaurantEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "set.restaurant.open.test.";
  }

  @Test
  void adminOpensAClosedRestaurant() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), false);

    setOpen(restaurantId, "role=ADMIN", openBody(true))
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.restaurant_id")
        .isEqualTo(restaurantId)
        .jsonPath("$.name")
        .isEqualTo("Koshary Corner")
        .jsonPath("$.is_open")
        .isEqualTo(true);

    assertThat(isOpenInDatabase(restaurantId)).isTrue();
  }

  @Test
  void restaurantClosesItself() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    setOpen(restaurantId, "role=RESTAURANT&restaurantId=" + restaurantId, openBody(false))
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.is_open")
        .isEqualTo(false);

    assertThat(isOpenInDatabase(restaurantId)).isFalse();
  }

  @Test
  void settingTheCurrentValueAgainSucceeds() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    setOpen(restaurantId, "role=ADMIN", openBody(true))
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.is_open")
        .isEqualTo(true);
  }

  @ParameterizedTest
  @ValueSource(strings = {"CUSTOMER", "COURIER", "SYSTEM"})
  void rejectsEveryRoleButAdminAndRestaurant(String role) {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), false);

    setOpen(restaurantId, "role=" + role, openBody(true))
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Role " + role + " may not open or close this restaurant");

    assertThat(isOpenInDatabase(restaurantId)).isFalse();
  }

  @Test
  void rejectsARestaurantOpeningAnother() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), false);
    long otherId = insertRestaurant("Other Corner", email("other"), true);

    setOpen(restaurantId, "role=RESTAURANT&restaurantId=" + otherId, openBody(true))
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Role RESTAURANT may not open or close this restaurant");
  }

  @Test
  void rejectsARestaurantWithoutItsId() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), false);

    setOpen(restaurantId, "role=RESTAURANT", openBody(true)).expectStatus().isForbidden();
  }

  @Test
  void checksTheRoleBeforeExistence() {
    setOpen(999_999L, "role=CUSTOMER", openBody(true)).expectStatus().isForbidden();
  }

  @Test
  void rejectsAnUnknownRestaurant() {
    setOpen(999_999L, "role=ADMIN", openBody(true))
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant not found");
  }

  @Test
  void rejectsASoftDeletedRestaurant() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), false);
    softDeleteRestaurant(restaurantId);

    setOpen(restaurantId, "role=ADMIN", openBody(true)).expectStatus().isNotFound();
  }

  @Test
  void rejectsAMissingIsOpen() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), false);

    setOpen(restaurantId, "role=ADMIN", "{}")
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .value(String.class, message -> assertThat(message).contains("isOpen"));
  }

  @Test
  void rejectsAMissingRole() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), false);

    client
        .put()
        .uri("/api/v1/restaurants/{id}/open", restaurantId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(openBody(true))
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("role is required");
  }

  private RestTestClient.ResponseSpec setOpen(long restaurantId, String query, String body) {
    return client
        .put()
        .uri("/api/v1/restaurants/{id}/open?" + query, restaurantId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .exchange();
  }

  private static String openBody(boolean isOpen) {
    return """
        { "is_open": %s }
        """
        .formatted(isOpen);
  }

  private boolean isOpenInDatabase(long restaurantId) {
    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject(
            "SELECT restaurant_is_open FROM restaurants WHERE restaurant_id = ?",
            Boolean.class,
            restaurantId));
  }
}
