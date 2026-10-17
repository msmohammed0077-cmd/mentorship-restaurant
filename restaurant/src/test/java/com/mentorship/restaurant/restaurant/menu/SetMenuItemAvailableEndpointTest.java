package com.mentorship.restaurant.restaurant.menu;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.RestaurantEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class SetMenuItemAvailableEndpointTest extends RestaurantEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "set.menu.item.available.test.";
  }

  @Test
  void ownerTakesAnItemOff() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "SMA-" + restaurantId, true);

    setAvailable(
            asRestaurantOwner(restaurantId), restaurantId, itemId, "{ \"is_available\": false }")
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.menu_item_id")
        .isEqualTo(itemId)
        .jsonPath("$.is_available")
        .isEqualTo(false);

    assertThat(isAvailableInDatabase(itemId)).isFalse();
  }

  @Test
  void ownerPutsAnItemBack() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "SMA-" + restaurantId, false);

    setAvailable(
            asRestaurantOwner(restaurantId), restaurantId, itemId, "{ \"is_available\": true }")
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.is_available")
        .isEqualTo(true);

    assertThat(isAvailableInDatabase(itemId)).isTrue();
  }

  @Test
  void settingTheCurrentValueAgainSucceeds() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "SMA-" + restaurantId, true);

    setAvailable(
            asRestaurantOwner(restaurantId), restaurantId, itemId, "{ \"is_available\": true }")
        .expectStatus()
        .isOk();
  }

  @Test
  void rejectsAMissingValue() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "SMA-" + restaurantId, true);

    setAvailable(asRestaurantOwner(restaurantId), restaurantId, itemId, "{}")
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .value(String.class, message -> assertThat(message).contains("isAvailable"));
  }

  @Test
  void rejectsAnotherRestaurantsItem() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    insertMenu(restaurantId);

    setAvailable(asRestaurantOwner(restaurantId), restaurantId, 1L, "{ \"is_available\": false }")
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Item not found");

    assertThat(isAvailableInDatabase(1L)).isTrue();
  }

  @Test
  void rejectsADeletedItem() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "SMA-" + restaurantId, true);
    softDeleteMenuItem(jdbcTemplate, itemId);

    setAvailable(
            asRestaurantOwner(restaurantId), restaurantId, itemId, "{ \"is_available\": false }")
        .expectStatus()
        .isNotFound();
  }

  @Test
  void refusesARequestWithoutAToken() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "SMA-" + restaurantId, true);

    setAvailable(client, restaurantId, itemId, "{ \"is_available\": false }")
        .expectStatus()
        .isUnauthorized();

    assertThat(isAvailableInDatabase(itemId)).isTrue();
  }

  @Test
  void refusesACustomer() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "SMA-" + restaurantId, true);

    setAvailable(asRole("CUSTOMER"), restaurantId, itemId, "{ \"is_available\": false }")
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Access denied");
  }

  @Test
  void refusesAnotherRestaurantsOwnerAndTheAdmin() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "SMA-" + restaurantId, true);
    long otherId = insertRestaurant("Other Corner", email("other"), true);

    for (RestTestClient caller : new RestTestClient[] {asRestaurantOwner(otherId), asAdmin()}) {
      setAvailable(caller, restaurantId, itemId, "{ \"is_available\": false }")
          .expectStatus()
          .isForbidden()
          .expectBody()
          .jsonPath("$.message")
          .isEqualTo("Restaurant belongs to another owner");
    }

    assertThat(isAvailableInDatabase(itemId)).isTrue();
  }

  private RestTestClient.ResponseSpec setAvailable(
      RestTestClient caller, long restaurantId, long itemId, String body) {
    return caller
        .put()
        .uri("/api/v1/restaurants/{id}/menu/items/{itemId}/available", restaurantId, itemId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .exchange();
  }

  private boolean isAvailableInDatabase(long itemId) {
    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject(
            "SELECT menu_item_is_available FROM menu_items WHERE menu_item_id = ?",
            Boolean.class,
            itemId));
  }
}
