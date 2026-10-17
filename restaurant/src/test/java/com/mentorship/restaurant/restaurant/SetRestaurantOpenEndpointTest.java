package com.mentorship.restaurant.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.RestaurantEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class SetRestaurantOpenEndpointTest extends RestaurantEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "set.restaurant.open.test.";
  }

  @Test
  void ownerOpensAClosedRestaurant() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), false);

    setOpen(asRestaurantOwner(restaurantId), restaurantId, openBody(true))
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
  void ownerClosesAnOpenRestaurant() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    setOpen(asRestaurantOwner(restaurantId), restaurantId, openBody(false))
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

    setOpen(asRestaurantOwner(restaurantId), restaurantId, openBody(true))
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.is_open")
        .isEqualTo(true);
  }

  @Test
  void refusesARequestWithoutAToken() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), false);

    setOpen(client, restaurantId, openBody(true)).expectStatus().isUnauthorized();

    assertThat(isOpenInDatabase(restaurantId)).isFalse();
  }

  @Test
  void refusesACustomer() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), false);

    setOpen(asRole("CUSTOMER"), restaurantId, openBody(true))
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Access denied");

    assertThat(isOpenInDatabase(restaurantId)).isFalse();
  }

  @Test
  void refusesAnotherRestaurantsOwner() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), false);
    long otherId = insertRestaurant("Other Corner", email("other"), true);

    refusesNotTheOwner(asRestaurantOwner(otherId), restaurantId);
  }

  /** The admin holds restaurant.write but owns no restaurant: nothing bypasses ownership. */
  @Test
  void refusesTheAdmin() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), false);

    refusesNotTheOwner(asAdmin(), restaurantId);
  }

  /** Ownership is checked before existence, so an unknown id answers as another's does. */
  @Test
  void refusesAnUnknownRestaurant() {
    long ownId = insertRestaurant("Koshary Corner", email("koshary"), false);

    setOpen(asRestaurantOwner(ownId), 999_999L, openBody(true))
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant belongs to another owner");
  }

  @Test
  void rejectsItsOwnSoftDeletedRestaurant() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), false);
    softDeleteRestaurant(restaurantId);

    setOpen(asRestaurantOwner(restaurantId), restaurantId, openBody(true))
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant not found");
  }

  @Test
  void rejectsAMissingIsOpen() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), false);

    setOpen(asRestaurantOwner(restaurantId), restaurantId, "{}")
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .value(String.class, message -> assertThat(message).contains("isOpen"));
  }

  private void refusesNotTheOwner(RestTestClient caller, long restaurantId) {
    setOpen(caller, restaurantId, openBody(true))
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant belongs to another owner");

    assertThat(isOpenInDatabase(restaurantId)).isFalse();
  }

  private RestTestClient.ResponseSpec setOpen(
      RestTestClient caller, long restaurantId, String body) {
    return caller
        .put()
        .uri("/api/v1/restaurants/{id}/open", restaurantId)
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
