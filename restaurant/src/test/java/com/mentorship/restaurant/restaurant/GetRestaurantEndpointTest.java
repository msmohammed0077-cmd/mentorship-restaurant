package com.mentorship.restaurant.restaurant;

import com.mentorship.restaurant.support.RestaurantEndpointTestSupport;
import org.junit.jupiter.api.Test;

class GetRestaurantEndpointTest extends RestaurantEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "get.restaurant.test.";
  }

  @Test
  void returnsASeededRestaurantWithoutThePassword() {
    client
        .get()
        .uri("/api/v1/restaurants/1")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.restaurant_id")
        .isEqualTo(1)
        .jsonPath("$.name")
        .isEqualTo("Nile Kitchen")
        .jsonPath("$.description")
        .isEqualTo("Modern Egyptian and Mediterranean dishes.")
        .jsonPath("$.email")
        .isEqualTo("contact@nilekitchen.example.com")
        .jsonPath("$.is_open")
        .isEqualTo(true)
        .jsonPath("$.password")
        .doesNotExist()
        .jsonPath("$.menus")
        .doesNotExist();
  }

  @Test
  void returnsAClosedRestaurant() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), false);

    client
        .get()
        .uri("/api/v1/restaurants/{id}", restaurantId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.restaurant_id")
        .isEqualTo(restaurantId)
        .jsonPath("$.email")
        .isEqualTo(email("koshary"))
        .jsonPath("$.is_open")
        .isEqualTo(false);
  }

  @Test
  void rejectsAnUnknownRestaurant() {
    client
        .get()
        .uri("/api/v1/restaurants/999999")
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant not found");
  }

  @Test
  void rejectsASoftDeletedRestaurant() {
    long restaurantId = insertRestaurant("Koshary Corner", email("koshary"), true);
    softDeleteRestaurant(restaurantId);

    client
        .get()
        .uri("/api/v1/restaurants/{id}", restaurantId)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant not found");
  }
}
