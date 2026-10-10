package com.mentorship.restaurant.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.RestaurantEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    deleteRestaurant(restaurantId, "role=ADMIN").expectStatus().isNoContent();

    assertThat(isSoftDeleted(restaurantId)).isTrue();
    deleteRestaurant(restaurantId, "role=ADMIN")
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant not found");
  }

  @Test
  void hidesTheDeletedRestaurantsDetails() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    deleteRestaurant(restaurantId, "role=ADMIN").expectStatus().isNoContent();

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

    deleteRestaurant(restaurantId, "role=ADMIN").expectStatus().isNoContent();

    client
        .post()
        .uri("/api/v1/restaurants?role=ADMIN")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            { "name": "Koshary Corner", "email": "%s" }
            """
                .formatted(email("koshary")))
        .exchange()
        .expectStatus()
        .isCreated();
  }

  @Test
  void deletesARestaurantWhoseOrdersAreFinished() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    insertOrder(restaurantId, "DELIVERED");

    deleteRestaurant(restaurantId, "role=ADMIN").expectStatus().isNoContent();

    assertThat(isSoftDeleted(restaurantId)).isTrue();
  }

  @Test
  void rejectsARestaurantWithAnActiveOrder() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    insertOrder(restaurantId, "PLACED");

    deleteRestaurant(restaurantId, "role=ADMIN")
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant has active orders");

    assertThat(isSoftDeleted(restaurantId)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"CUSTOMER", "RESTAURANT", "COURIER", "SYSTEM"})
  void rejectsEveryRoleButAdmin(String role) {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    deleteRestaurant(restaurantId, "role=" + role)
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Role " + role + " may not delete a restaurant");

    assertThat(isSoftDeleted(restaurantId)).isFalse();
  }

  @Test
  void rejectsARestaurantDeletingItself() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    deleteRestaurant(restaurantId, "role=RESTAURANT&restaurantId=" + restaurantId)
        .expectStatus()
        .isForbidden();

    assertThat(isSoftDeleted(restaurantId)).isFalse();
  }

  @Test
  void checksTheRoleBeforeExistence() {
    deleteRestaurant(999_999L, "role=CUSTOMER").expectStatus().isForbidden();
  }

  @Test
  void rejectsAnUnknownRestaurant() {
    deleteRestaurant(999_999L, "role=ADMIN")
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant not found");
  }

  @Test
  void rejectsAMissingRole() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);

    client
        .delete()
        .uri("/api/v1/restaurants/{id}", restaurantId)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("role is required");
  }

  private RestTestClient.ResponseSpec deleteRestaurant(long restaurantId, String query) {
    return client.delete().uri("/api/v1/restaurants/{id}?" + query, restaurantId).exchange();
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
