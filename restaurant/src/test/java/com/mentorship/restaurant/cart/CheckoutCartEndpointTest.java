package com.mentorship.restaurant.cart;

import static com.mentorship.restaurant.support.RestaurantEndpointTestSupport.insertMenuItem;
import static com.mentorship.restaurant.support.RestaurantEndpointTestSupport.insertRestaurant;
import static com.mentorship.restaurant.support.RestaurantEndpointTestSupport.softDeleteRestaurant;
import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.CartEndpointTestSupport;
import org.junit.jupiter.api.Test;

class CheckoutCartEndpointTest extends CartEndpointTestSupport {

  @Test
  void acknowledgesSuccessfulPaymentAndUpdatesStock() {
    long cartId = createCartWithItem(KOFTA, 2);

    client
        .post()
        .uri("/api/v1/cart/{cartId}/checkout", cartId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("SUCCESS")
        .jsonPath("$.message")
        .isEqualTo("Payment successful");

    assertThat(stockFor(KOFTA)).isEqualTo(48);
    assertThat(cartItemCountFor(cartId)).isZero();
  }

  @Test
  void rejectsAnEmptyCart() {
    long cartId = createCartWithItem(KOFTA, 2);
    client.delete().uri("/api/v1/cart/{cartId}", cartId).exchange().expectStatus().isOk();

    client
        .post()
        .uri("/api/v1/cart/{cartId}/checkout", cartId)
        .exchange()
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Cart is empty");
  }

  @Test
  void rejectsACartFromAClosedRestaurant() {
    long cartId = insertCartWithItem(WINGS, 1);

    client
        .post()
        .uri("/api/v1/cart/{cartId}/checkout", cartId)
        .exchange()
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant is closed");

    assertThat(stockFor(WINGS)).isEqualTo(30);
    assertThat(cartItemCountFor(cartId)).isEqualTo(1);
  }

  @Test
  void rejectsACartFromADeletedRestaurant() {
    long restaurantId =
        insertRestaurant(jdbcTemplate, "Deleted Grill", EMAIL_PREFIX + "grill@example.com", true);
    long menuItemId = insertMenuItem(jdbcTemplate, restaurantId);
    long cartId = insertCartWithItem(menuItemId, 1);
    softDeleteRestaurant(jdbcTemplate, restaurantId);

    client
        .post()
        .uri("/api/v1/cart/{cartId}/checkout", cartId)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant not found");

    assertThat(stockFor(menuItemId)).isEqualTo(50);
    assertThat(cartItemCountFor(cartId)).isEqualTo(1);
  }

  @Test
  void rejectsAnUnknownCart() {
    client
        .post()
        .uri("/api/v1/cart/{cartId}/checkout", 999999L)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Cart not found");
  }

  @Test
  void leavesEverythingUntouchedWhenAnItemIsOutOfStock() {
    long cartId = createCartWithItem(KOFTA, 2);
    addItem(FALAFEL, 2);
    setStock(KOFTA, 0);

    client
        .post()
        .uri("/api/v1/cart/{cartId}/checkout", cartId)
        .exchange()
        .expectStatus()
        .isEqualTo(409);

    assertThat(stockFor(KOFTA)).isZero();
    assertThat(stockFor(FALAFEL)).isEqualTo(75);
    assertThat(cartItemCountFor(cartId)).isEqualTo(2);
  }
}
