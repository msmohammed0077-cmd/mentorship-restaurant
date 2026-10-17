package com.mentorship.restaurant.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.RestaurantEndpointTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class DeleteRestaurantEndpointTest extends RestaurantEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "delete.restaurant.test.";
  }

  @Test
  void adminDeletesTheRestaurantSoItIsNoLongerFound() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    deleteRestaurant(asAdmin(), restaurantId).expectStatus().isNoContent();

    assertThat(isSoftDeleted(restaurantId)).isTrue();
    deleteRestaurant(asAdmin(), restaurantId)
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant not found");
  }

  @Test
  void hidesTheDeletedRestaurantsDetails() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    deleteRestaurant(asAdmin(), restaurantId).expectStatus().isNoContent();

    client
        .get()
        .uri("/api/v1/restaurants/{id}", restaurantId)
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void freesItsEmailForANewRestaurant() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    deleteRestaurant(asAdmin(), restaurantId).expectStatus().isNoContent();

    asAdmin()
        .post()
        .uri("/api/v1/restaurants")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            { "name": "Koshary Corner", "email": "%s", "owner_keycloak_id": "%s" }
            """
                .formatted(email("koshary"), UUID.randomUUID()))
        .exchange()
        .expectStatus()
        .isCreated();
  }

  @Test
  void deletesARestaurantWhoseOrdersAreFinished() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    insertOrder(restaurantId, "DELIVERED");

    deleteRestaurant(asAdmin(), restaurantId).expectStatus().isNoContent();

    assertThat(isSoftDeleted(restaurantId)).isTrue();
  }

  @Test
  void rejectsARestaurantWithAnActiveOrder() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    insertOrder(restaurantId, "PLACED");

    deleteRestaurant(asAdmin(), restaurantId)
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant has active orders");

    assertThat(isSoftDeleted(restaurantId)).isFalse();
  }

  @Test
  void refusesARequestWithoutAToken() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    deleteRestaurant(client, restaurantId).expectStatus().isUnauthorized();

    assertThat(isSoftDeleted(restaurantId)).isFalse();
  }

  @Test
  void refusesACustomer() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    refusesWithoutThePermission(asRole("CUSTOMER"), restaurantId);
  }

  @Test
  void refusesTheRestaurantsOwner() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    refusesWithoutThePermission(asRestaurantOwner(restaurantId), restaurantId);
  }

  @Test
  void checksThePermissionBeforeExistence() {
    long ownId = insertRestaurant("Koshary Corner", email("koshary"), true);

    deleteRestaurant(asRestaurantOwner(ownId), 999_999L).expectStatus().isForbidden();
  }

  @Test
  void rejectsAnUnknownRestaurant() {
    deleteRestaurant(asAdmin(), 999_999L)
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant not found");
  }

  private void refusesWithoutThePermission(RestTestClient caller, long restaurantId) {
    deleteRestaurant(caller, restaurantId)
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Access denied");

    assertThat(isSoftDeleted(restaurantId)).isFalse();
  }

  private RestTestClient.ResponseSpec deleteRestaurant(RestTestClient caller, long restaurantId) {
    return caller.delete().uri("/api/v1/restaurants/{id}", restaurantId).exchange();
  }

  private boolean isSoftDeleted(long restaurantId) {
    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject(
            """
            SELECT u.user_deleted_at IS NOT NULL
            FROM users u JOIN restaurants r ON r.user_id = u.user_id
            WHERE r.restaurant_id = ?
            """,
            Boolean.class,
            restaurantId));
  }
}
