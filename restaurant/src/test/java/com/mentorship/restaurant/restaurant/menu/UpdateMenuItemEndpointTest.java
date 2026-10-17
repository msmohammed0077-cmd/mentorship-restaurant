package com.mentorship.restaurant.restaurant.menu;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.RestaurantEndpointTestSupport;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class UpdateMenuItemEndpointTest extends RestaurantEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "update.menu.item.test.";
  }

  @Test
  void ownerUpdatesEveryField() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "UMI-" + restaurantId, true);

    update(
            asRestaurantOwner(restaurantId),
            restaurantId,
            itemId,
            """
            {
              "code": "UMI-NEW-%d",
              "name": "Koshary Plate",
              "price": 90.25,
              "stock": 7,
              "note": "Large.",
              "image_url": "https://example.com/plate.jpg"
            }
            """
                .formatted(restaurantId))
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.menu_item_id")
        .isEqualTo(itemId)
        .jsonPath("$.code")
        .isEqualTo("UMI-NEW-" + restaurantId)
        .jsonPath("$.name")
        .isEqualTo("Koshary Plate")
        .jsonPath("$.price")
        .isEqualTo(90.25)
        .jsonPath("$.stock")
        .isEqualTo(7)
        .jsonPath("$.note")
        .isEqualTo("Large.")
        .jsonPath("$.image_url")
        .isEqualTo("https://example.com/plate.jpg");

    assertThat(storedItem(itemId))
        .containsEntry("menu_item_name", "Koshary Plate")
        .containsEntry("menu_item_price", new BigDecimal("90.25"))
        .containsEntry("menu_item_stock", 7);
  }

  @Test
  void leavesAbsentAndNullFieldsUnchanged() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "UMI-" + restaurantId, true);

    update(asRestaurantOwner(restaurantId), restaurantId, itemId, "{ \"name\": null }")
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.name")
        .isEqualTo("Test Dish")
        .jsonPath("$.price")
        .isEqualTo(100.00)
        .jsonPath("$.stock")
        .isEqualTo(50);
  }

  /** Availability has its own endpoint; an is_available here is ignored. */
  @Test
  void ignoresAvailability() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "UMI-" + restaurantId, true);

    update(asRestaurantOwner(restaurantId), restaurantId, itemId, "{ \"is_available\": false }")
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.is_available")
        .isEqualTo(true);
  }

  @Test
  void acceptsItsOwnCode() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "UMI-" + restaurantId, true);

    update(asRestaurantOwner(restaurantId), restaurantId, itemId, codeBody("UMI-" + restaurantId))
        .expectStatus()
        .isOk();
  }

  @Test
  void rejectsATakenCode() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "UMI-" + restaurantId, true);

    update(asRestaurantOwner(restaurantId), restaurantId, itemId, codeBody("NK-KOFTA-01"))
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Item code is already in use");

    assertThat(storedItem(itemId)).containsEntry("menu_item_code", "UMI-" + restaurantId);
  }

  /** The item must be on the restaurant's menu: another restaurant's item is not found. */
  @Test
  void rejectsAnotherRestaurantsItem() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    insertMenu(restaurantId);

    update(asRestaurantOwner(restaurantId), restaurantId, 1L, codeBody("UMI-STOLEN"))
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Item not found");
  }

  @Test
  void rejectsADeletedItem() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "UMI-" + restaurantId, true);
    softDeleteMenuItem(jdbcTemplate, itemId);

    update(asRestaurantOwner(restaurantId), restaurantId, itemId, codeBody("UMI-GONE"))
        .expectStatus()
        .isNotFound();
  }

  @Test
  void rejectsAnItemOfADeletedMenu() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long menuId = insertMenu(restaurantId);
    long itemId = insertItem(menuId, "UMI-" + restaurantId, true);
    softDeleteMenu(menuId);

    update(asRestaurantOwner(restaurantId), restaurantId, itemId, codeBody("UMI-GONE"))
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Menu not found");
  }

  @Test
  void refusesARequestWithoutAToken() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "UMI-" + restaurantId, true);

    update(client, restaurantId, itemId, codeBody("UMI-ANON")).expectStatus().isUnauthorized();
  }

  @Test
  void refusesACustomer() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "UMI-" + restaurantId, true);

    update(asRole("CUSTOMER"), restaurantId, itemId, codeBody("UMI-CUSTOMER"))
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Access denied");
  }

  @Test
  void refusesAnotherRestaurantsOwnerAndTheAdmin() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "UMI-" + restaurantId, true);
    long otherId = insertRestaurant("Other Corner", email("other"), true);

    for (RestTestClient caller : new RestTestClient[] {asRestaurantOwner(otherId), asAdmin()}) {
      update(caller, restaurantId, itemId, codeBody("UMI-STOLEN"))
          .expectStatus()
          .isForbidden()
          .expectBody()
          .jsonPath("$.message")
          .isEqualTo("Restaurant belongs to another owner");
    }

    assertThat(storedItem(itemId)).containsEntry("menu_item_code", "UMI-" + restaurantId);
  }

  @Test
  void rejectsANegativePrice() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "UMI-" + restaurantId, true);

    update(asRestaurantOwner(restaurantId), restaurantId, itemId, "{ \"price\": -1 }")
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .value(String.class, message -> assertThat(message).contains("price"));
  }

  @Test
  void rejectsABlankName() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long itemId = insertItem(insertMenu(restaurantId), "UMI-" + restaurantId, true);

    update(asRestaurantOwner(restaurantId), restaurantId, itemId, "{ \"name\": \"  \" }")
        .expectStatus()
        .isBadRequest();
  }

  private RestTestClient.ResponseSpec update(
      RestTestClient caller, long restaurantId, long itemId, String body) {
    return caller
        .patch()
        .uri("/api/v1/restaurants/{id}/menu/items/{itemId}", restaurantId, itemId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .exchange();
  }

  private static String codeBody(String code) {
    return """
        { "code": "%s" }
        """
        .formatted(code);
  }

  private Map<String, Object> storedItem(long itemId) {
    return jdbcTemplate.queryForMap(
        """
        SELECT menu_item_code, menu_item_name, menu_item_price, menu_item_stock
        FROM menu_items WHERE menu_item_id = ?
        """,
        itemId);
  }
}
