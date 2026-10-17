package com.mentorship.restaurant.restaurant.menu;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.RestaurantEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class CreateMenuEndpointTest extends RestaurantEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "create.menu.test.";
  }

  @Test
  void ownerCreatesTheMenu() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    create(asRestaurantOwner(restaurantId), restaurantId, body("CM-" + restaurantId, "Mains"))
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.menu_id")
        .isNotEmpty()
        .jsonPath("$.restaurant_id")
        .isEqualTo(restaurantId)
        .jsonPath("$.code")
        .isEqualTo("CM-" + restaurantId)
        .jsonPath("$.category")
        .isEqualTo("Mains")
        .jsonPath("$.items.length()")
        .isEqualTo(0);

    assertThat(activeMenus(restaurantId)).isOne();
  }

  @Test
  void refusesARequestWithoutAToken() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    create(client, restaurantId, body("CM-" + restaurantId, "Mains"))
        .expectStatus()
        .isUnauthorized();

    assertThat(activeMenus(restaurantId)).isZero();
  }

  @Test
  void refusesACustomer() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    create(asRole("CUSTOMER"), restaurantId, body("CM-" + restaurantId, "Mains"))
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Access denied");
  }

  @Test
  void refusesAnotherRestaurantsOwner() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    long otherId = insertRestaurant("Other Corner", email("other"), true);

    refusesNotTheOwner(asRestaurantOwner(otherId), restaurantId);
  }

  /** The admin holds menu.write but owns no restaurant: nothing bypasses ownership. */
  @Test
  void refusesTheAdmin() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    refusesNotTheOwner(asAdmin(), restaurantId);
  }

  @Test
  void refusesAnUnknownRestaurant() {
    long ownId = insertRestaurant("Koshary Corner", email("koshary"), true);

    create(asRestaurantOwner(ownId), 999_999L, body("CM-UNKNOWN", "Mains"))
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant belongs to another owner");
  }

  @Test
  void rejectsItsOwnDeletedRestaurant() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    softDeleteRestaurant(restaurantId);

    create(asRestaurantOwner(restaurantId), restaurantId, body("CM-" + restaurantId, "Mains"))
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant not found");
  }

  @Test
  void rejectsASecondMenu() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    insertMenu(restaurantId);

    create(asRestaurantOwner(restaurantId), restaurantId, body("CM-" + restaurantId, "Mains"))
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant already has a menu");

    assertThat(activeMenus(restaurantId)).isOne();
  }

  @Test
  void createsANewMenuAfterTheOldOneIsDeleted() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    softDeleteMenu(insertMenu(restaurantId));

    create(asRestaurantOwner(restaurantId), restaurantId, body("CM-" + restaurantId, "Mains"))
        .expectStatus()
        .isCreated();

    assertThat(activeMenus(restaurantId)).isOne();
  }

  @Test
  void rejectsATakenCode() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    rejectsCode(restaurantId, "NK-MAIN");
  }

  /** Codes are unique across every row, as V1's constraint is: a deleted menu keeps its code. */
  @Test
  void rejectsTheCodeOfADeletedMenu() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    softDeleteMenu(insertMenu(restaurantId));

    rejectsCode(restaurantId, "TEST-MENU-" + restaurantId);
  }

  @Test
  void rejectsABlankCode() {
    rejectsBody(body("   ", "Mains"), "code");
  }

  @Test
  void rejectsACodeOver100Characters() {
    rejectsBody(body("x".repeat(101), "Mains"), "code");
  }

  @Test
  void rejectsAMissingCategory() {
    rejectsBody(
        """
        { "code": "CM-NO-CATEGORY" }
        """,
        "category");
  }

  private void refusesNotTheOwner(RestTestClient caller, long restaurantId) {
    create(caller, restaurantId, body("CM-" + restaurantId, "Mains"))
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant belongs to another owner");

    assertThat(activeMenus(restaurantId)).isZero();
  }

  private void rejectsCode(long restaurantId, String code) {
    create(asRestaurantOwner(restaurantId), restaurantId, body(code, "Mains"))
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Menu code is already in use");

    assertThat(activeMenus(restaurantId)).isZero();
  }

  private void rejectsBody(String body, String field) {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

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
        .uri("/api/v1/restaurants/{id}/menu", restaurantId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .exchange();
  }

  private static String body(String code, String category) {
    return """
        { "code": "%s", "category": "%s" }
        """
        .formatted(code, category);
  }

  private int activeMenus(long restaurantId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM menus WHERE restaurant_id = ? AND menu_deleted_at IS NULL",
            Integer.class,
            restaurantId);
    return count == null ? 0 : count;
  }
}
