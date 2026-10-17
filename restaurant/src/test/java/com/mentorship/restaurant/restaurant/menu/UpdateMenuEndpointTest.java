package com.mentorship.restaurant.restaurant.menu;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.RestaurantEndpointTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class UpdateMenuEndpointTest extends RestaurantEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "update.menu.test.";
  }

  @Test
  void ownerUpdatesCodeAndCategoryAndSeesTheItems() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long menuId = insertMenu(restaurantId);
    insertItem(menuId, "UM-ITEM-" + menuId, true);

    update(
            asRestaurantOwner(restaurantId),
            restaurantId,
            """
            { "code": "UM-%d", "category": "Sandwiches" }
            """
                .formatted(restaurantId))
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.menu_id")
        .isEqualTo(menuId)
        .jsonPath("$.code")
        .isEqualTo("UM-" + restaurantId)
        .jsonPath("$.category")
        .isEqualTo("Sandwiches")
        .jsonPath("$.items.length()")
        .isEqualTo(1)
        .jsonPath("$.items[0].category")
        .isEqualTo("Sandwiches");

    assertThat(storedMenu(menuId))
        .containsEntry("menu_code", "UM-" + restaurantId)
        .containsEntry("menu_category", "Sandwiches");
  }

  @Test
  void leavesAbsentAndNullFieldsUnchanged() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long menuId = insertMenu(restaurantId);

    update(asRestaurantOwner(restaurantId), restaurantId, "{ \"code\": null }")
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo("TEST-MENU-" + restaurantId)
        .jsonPath("$.category")
        .isEqualTo("Main");

    assertThat(storedMenu(menuId)).containsEntry("menu_code", "TEST-MENU-" + restaurantId);
  }

  @Test
  void acceptsItsOwnCode() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    insertMenu(restaurantId);

    update(asRestaurantOwner(restaurantId), restaurantId, codeBody("TEST-MENU-" + restaurantId))
        .expectStatus()
        .isOk();
  }

  @Test
  void rejectsATakenCode() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long menuId = insertMenu(restaurantId);

    update(asRestaurantOwner(restaurantId), restaurantId, codeBody("NK-MAIN"))
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Menu code is already in use");

    assertThat(storedMenu(menuId)).containsEntry("menu_code", "TEST-MENU-" + restaurantId);
  }

  @Test
  void rejectsARestaurantWithoutAMenu() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    update(asRestaurantOwner(restaurantId), restaurantId, codeBody("UM-NONE"))
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

    update(asRestaurantOwner(restaurantId), restaurantId, codeBody("UM-DELETED"))
        .expectStatus()
        .isNotFound();
  }

  @Test
  void refusesARequestWithoutAToken() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    insertMenu(restaurantId);

    update(client, restaurantId, codeBody("UM-ANON")).expectStatus().isUnauthorized();
  }

  @Test
  void refusesACustomer() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    insertMenu(restaurantId);

    update(asRole("CUSTOMER"), restaurantId, codeBody("UM-CUSTOMER"))
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Access denied");
  }

  @Test
  void refusesAnotherRestaurantsOwnerAndTheAdmin() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long menuId = insertMenu(restaurantId);
    long otherId = insertRestaurant("Other Corner", email("other"), true);

    for (RestTestClient caller : new RestTestClient[] {asRestaurantOwner(otherId), asAdmin()}) {
      update(caller, restaurantId, codeBody("UM-STOLEN"))
          .expectStatus()
          .isForbidden()
          .expectBody()
          .jsonPath("$.message")
          .isEqualTo("Restaurant belongs to another owner");
    }

    assertThat(storedMenu(menuId)).containsEntry("menu_code", "TEST-MENU-" + restaurantId);
  }

  @Test
  void rejectsABlankCode() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    insertMenu(restaurantId);

    update(asRestaurantOwner(restaurantId), restaurantId, codeBody("   "))
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .value(String.class, message -> assertThat(message).contains("code"));
  }

  private RestTestClient.ResponseSpec update(
      RestTestClient caller, long restaurantId, String body) {
    return caller
        .patch()
        .uri("/api/v1/restaurants/{id}/menu", restaurantId)
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

  private Map<String, Object> storedMenu(long menuId) {
    return jdbcTemplate.queryForMap(
        "SELECT menu_code, menu_category FROM menus WHERE menu_id = ?", menuId);
  }
}
