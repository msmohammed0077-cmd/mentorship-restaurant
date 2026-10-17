package com.mentorship.restaurant.restaurant.menu;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.RestaurantEndpointTestSupport;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class CreateMenuItemEndpointTest extends RestaurantEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "create.menu.item.test.";
  }

  @Test
  void ownerCreatesAnAvailableItem() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    insertMenu(restaurantId);
    String code = "CMI-" + restaurantId;

    create(
            asRestaurantOwner(restaurantId),
            restaurantId,
            """
            {
              "code": "%s",
              "name": "Koshary Bowl",
              "price": 85.50,
              "stock": 20,
              "note": "With crispy onions.",
              "image_url": "https://example.com/koshary.jpg"
            }
            """
                .formatted(code))
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.menu_item_id")
        .isNotEmpty()
        .jsonPath("$.code")
        .isEqualTo(code)
        .jsonPath("$.name")
        .isEqualTo("Koshary Bowl")
        .jsonPath("$.category")
        .isEqualTo("Main")
        .jsonPath("$.price")
        .isEqualTo(85.50)
        .jsonPath("$.stock")
        .isEqualTo(20)
        .jsonPath("$.note")
        .isEqualTo("With crispy onions.")
        .jsonPath("$.image_url")
        .isEqualTo("https://example.com/koshary.jpg")
        .jsonPath("$.is_available")
        .isEqualTo(true);

    assertThat(storedItem(code))
        .containsEntry("menu_item_name", "Koshary Bowl")
        .containsEntry("menu_item_price", new BigDecimal("85.50"))
        .containsEntry("menu_item_is_available", true);
  }

  @Test
  void createsAnUnavailableItem() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    insertMenu(restaurantId);

    create(
            asRestaurantOwner(restaurantId),
            restaurantId,
            """
            { "code": "CMI-%d", "name": "Koshary Bowl", "price": 85, "stock": 0,
              "is_available": false }
            """
                .formatted(restaurantId))
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.is_available")
        .isEqualTo(false)
        .jsonPath("$.note")
        .doesNotExist();
  }

  @Test
  void rejectsARestaurantWithoutAMenu() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    create(asRestaurantOwner(restaurantId), restaurantId, body("CMI-" + restaurantId))
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Menu not found");
  }

  @Test
  void rejectsATakenCode() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    insertMenu(restaurantId);

    create(asRestaurantOwner(restaurantId), restaurantId, body("NK-KOFTA-01"))
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Item code is already in use");
  }

  @Test
  void refusesARequestWithoutAToken() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    insertMenu(restaurantId);

    create(client, restaurantId, body("CMI-" + restaurantId)).expectStatus().isUnauthorized();

    assertThat(itemsWithCode("CMI-" + restaurantId)).isZero();
  }

  @Test
  void refusesACustomer() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    insertMenu(restaurantId);

    create(asRole("CUSTOMER"), restaurantId, body("CMI-" + restaurantId))
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Access denied");
  }

  @Test
  void refusesAnotherRestaurantsOwnerAndTheAdmin() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    insertMenu(restaurantId);
    long otherId = insertRestaurant("Other Corner", email("other"), true);

    for (RestTestClient caller : new RestTestClient[] {asRestaurantOwner(otherId), asAdmin()}) {
      create(caller, restaurantId, body("CMI-" + restaurantId))
          .expectStatus()
          .isForbidden()
          .expectBody()
          .jsonPath("$.message")
          .isEqualTo("Restaurant belongs to another owner");
    }

    assertThat(itemsWithCode("CMI-" + restaurantId)).isZero();
  }

  @Test
  void rejectsAMissingName() {
    rejectsBody("{ \"code\": \"CMI-X\", \"price\": 1, \"stock\": 1 }", "name");
  }

  @Test
  void rejectsAMissingCode() {
    rejectsBody("{ \"name\": \"Dish\", \"price\": 1, \"stock\": 1 }", "code");
  }

  @Test
  void rejectsANegativePrice() {
    rejectsBody(
        "{ \"code\": \"CMI-X\", \"name\": \"Dish\", \"price\": -1, \"stock\": 1 }", "price");
  }

  @Test
  void rejectsAPriceWithThreeDecimals() {
    rejectsBody(
        "{ \"code\": \"CMI-X\", \"name\": \"Dish\", \"price\": 1.005, \"stock\": 1 }", "price");
  }

  @Test
  void rejectsANegativeStock() {
    rejectsBody(
        "{ \"code\": \"CMI-X\", \"name\": \"Dish\", \"price\": 1, \"stock\": -1 }", "stock");
  }

  @Test
  void rejectsAMissingStock() {
    rejectsBody("{ \"code\": \"CMI-X\", \"name\": \"Dish\", \"price\": 1 }", "stock");
  }

  private void rejectsBody(String body, String field) {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    insertMenu(restaurantId);

    create(asRestaurantOwner(restaurantId), restaurantId, body)
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .value(String.class, message -> assertThat(message).contains(field));
  }

  private RestTestClient.ResponseSpec create(
      RestTestClient caller, long restaurantId, String body) {
    return caller
        .post()
        .uri("/api/v1/restaurants/{id}/menu/items", restaurantId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .exchange();
  }

  private static String body(String code) {
    return """
        { "code": "%s", "name": "Koshary Bowl", "price": 85, "stock": 20 }
        """
        .formatted(code);
  }

  private Map<String, Object> storedItem(String code) {
    return jdbcTemplate.queryForMap(
        """
        SELECT menu_item_name, menu_item_price, menu_item_is_available
        FROM menu_items WHERE menu_item_code = ?
        """,
        code);
  }

  private int itemsWithCode(String code) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM menu_items WHERE menu_item_code = ?", Integer.class, code);
    return count == null ? 0 : count;
  }
}
