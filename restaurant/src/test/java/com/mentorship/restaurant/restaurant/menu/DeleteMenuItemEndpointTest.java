package com.mentorship.restaurant.restaurant.menu;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.RestaurantEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.client.RestTestClient;

class DeleteMenuItemEndpointTest extends RestaurantEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "delete.menu.item.test.";
  }

  @Test
  void ownerSoftDeletesTheItemSoTheMenuNoLongerListsIt() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "DMI-" + restaurantId, true);

    delete(asRestaurantOwner(restaurantId), restaurantId, itemId).expectStatus().isNoContent();

    assertThat(isDeleted(itemId)).isTrue();
    client
        .get()
        .uri("/api/v1/restaurants/{id}/menu", restaurantId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.items.length()")
        .isEqualTo(0);
    delete(asRestaurantOwner(restaurantId), restaurantId, itemId)
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Item not found");
  }

  @Test
  void rejectsAnotherRestaurantsItem() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    insertMenu(restaurantId);

    delete(asRestaurantOwner(restaurantId), restaurantId, 1L).expectStatus().isNotFound();

    assertThat(isDeleted(1L)).isFalse();
  }

  @Test
  void refusesARequestWithoutAToken() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "DMI-" + restaurantId, true);

    delete(client, restaurantId, itemId).expectStatus().isUnauthorized();

    assertThat(isDeleted(itemId)).isFalse();
  }

  @Test
  void refusesACustomer() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "DMI-" + restaurantId, true);

    delete(asRole("CUSTOMER"), restaurantId, itemId)
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Access denied");
  }

  @Test
  void refusesAnotherRestaurantsOwnerAndTheAdmin() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "DMI-" + restaurantId, true);
    long otherId = insertRestaurant("Other Corner", email("other"), true);

    for (RestTestClient caller : new RestTestClient[] {asRestaurantOwner(otherId), asAdmin()}) {
      delete(caller, restaurantId, itemId)
          .expectStatus()
          .isForbidden()
          .expectBody()
          .jsonPath("$.message")
          .isEqualTo("Restaurant belongs to another owner");
    }

    assertThat(isDeleted(itemId)).isFalse();
  }

  private RestTestClient.ResponseSpec delete(
      RestTestClient caller, long restaurantId, long itemId) {
    return caller
        .delete()
        .uri("/api/v1/restaurants/{id}/menu/items/{itemId}", restaurantId, itemId)
        .exchange();
  }

  private boolean isDeleted(long itemId) {
    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject(
            "SELECT menu_item_deleted_at IS NOT NULL FROM menu_items WHERE menu_item_id = ?",
            Boolean.class,
            itemId));
  }
}
