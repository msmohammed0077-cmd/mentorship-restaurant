package com.mentorship.restaurant.restaurant.menu;

import com.mentorship.restaurant.support.RestaurantEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.client.RestTestClient;

class GetMenuEndpointTest extends RestaurantEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "get.menu.test.";
  }

  @Test
  void returnsTheMenuAndEveryItemNotDeleted() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long menuId = insertMenu(restaurantId);
    long available = insertItem(menuId, "GET-MENU-A-" + menuId, true);
    long unavailable = insertItem(menuId, "GET-MENU-B-" + menuId, false);
    softDeleteMenuItem(jdbcTemplate, insertItem(menuId, "GET-MENU-C-" + menuId, true));

    getMenu(restaurantId)
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.menu_id")
        .isEqualTo(menuId)
        .jsonPath("$.restaurant_id")
        .isEqualTo(restaurantId)
        .jsonPath("$.code")
        .isEqualTo("TEST-MENU-" + restaurantId)
        .jsonPath("$.category")
        .isEqualTo("Main")
        .jsonPath("$.items.length()")
        .isEqualTo(2)
        .jsonPath("$.items[0].menu_item_id")
        .isEqualTo(available)
        .jsonPath("$.items[0].name")
        .isEqualTo("Test Dish")
        .jsonPath("$.items[0].category")
        .isEqualTo("Main")
        .jsonPath("$.items[0].price")
        .isEqualTo(100.00)
        .jsonPath("$.items[0].stock")
        .isEqualTo(50)
        .jsonPath("$.items[0].is_available")
        .isEqualTo(true)
        .jsonPath("$.items[1].menu_item_id")
        .isEqualTo(unavailable)
        .jsonPath("$.items[1].is_available")
        .isEqualTo(false);
  }

  @Test
  void readsASeededMenu() {
    getMenu(1L)
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo("NK-MAIN")
        .jsonPath("$.items[0].code")
        .isEqualTo("NK-KOFTA-01");
  }

  /** Public paths ignore tokens, so a bad one is not a 401. */
  @Test
  void ignoresAnInvalidToken() {
    client
        .get()
        .uri("/api/v1/restaurants/{id}/menu", 1L)
        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-token")
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void rejectsARestaurantWithoutAMenu() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    getMenu(restaurantId)
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Menu not found");
  }

  @Test
  void rejectsADeletedMenu() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    softDeleteMenu(insertMenu(restaurantId));

    getMenu(restaurantId)
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Menu not found");
  }

  @Test
  void rejectsAnUnknownRestaurant() {
    getMenu(999_999L)
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant not found");
  }

  @Test
  void rejectsADeletedRestaurant() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    insertMenu(restaurantId);
    softDeleteRestaurant(restaurantId);

    getMenu(restaurantId)
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant not found");
  }

  private RestTestClient.ResponseSpec getMenu(long restaurantId) {
    return client.get().uri("/api/v1/restaurants/{id}/menu", restaurantId).exchange();
  }
}
