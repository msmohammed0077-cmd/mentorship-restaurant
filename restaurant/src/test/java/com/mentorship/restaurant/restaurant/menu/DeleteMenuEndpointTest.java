package com.mentorship.restaurant.restaurant.menu;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.RestaurantEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.client.RestTestClient;

class DeleteMenuEndpointTest extends RestaurantEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "delete.menu.test.";
  }

  @Test
  void ownerSoftDeletesTheMenuAndItsItems() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long menuId = insertMenu(restaurantId);
    long itemId = insertItem(menuId, "DM-ITEM-" + menuId, true);

    delete(asRestaurantOwner(restaurantId), restaurantId).expectStatus().isNoContent();

    assertThat(isDeleted("menus", "menu", menuId)).isTrue();
    assertThat(isDeleted("menu_items", "menu_item", itemId)).isTrue();
    delete(asRestaurantOwner(restaurantId), restaurantId)
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Menu not found");
  }

  @Test
  void rejectsARestaurantWithoutAMenu() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    delete(asRestaurantOwner(restaurantId), restaurantId).expectStatus().isNotFound();
  }

  @Test
  void refusesARequestWithoutAToken() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long menuId = insertMenu(restaurantId);

    delete(client, restaurantId).expectStatus().isUnauthorized();

    assertThat(isDeleted("menus", "menu", menuId)).isFalse();
  }

  @Test
  void refusesACustomer() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long menuId = insertMenu(restaurantId);

    delete(asRole("CUSTOMER"), restaurantId)
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Access denied");

    assertThat(isDeleted("menus", "menu", menuId)).isFalse();
  }

  @Test
  void refusesAnotherRestaurantsOwnerAndTheAdmin() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long menuId = insertMenu(restaurantId);
    long otherId = insertRestaurant("Other Corner", email("other"), true);

    for (RestTestClient caller : new RestTestClient[] {asRestaurantOwner(otherId), asAdmin()}) {
      delete(caller, restaurantId)
          .expectStatus()
          .isForbidden()
          .expectBody()
          .jsonPath("$.message")
          .isEqualTo("Restaurant belongs to another owner");
    }

    assertThat(isDeleted("menus", "menu", menuId)).isFalse();
  }

  private RestTestClient.ResponseSpec delete(RestTestClient caller, long restaurantId) {
    return caller.delete().uri("/api/v1/restaurants/{id}/menu", restaurantId).exchange();
  }

  private boolean isDeleted(String table, String noun, long id) {
    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject(
            "SELECT %s_deleted_at IS NOT NULL FROM %s WHERE %s_id = ?".formatted(noun, table, noun),
            Boolean.class,
            id));
  }
}
