package com.mentorship.restaurant.cart;

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

  /** An empty cart is a state conflict, not a malformed request. */
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

  /**
   * Stock is decremented one line at a time, so a rejection partway through must undo the lines
   * already taken. Asserted on both items because the order the lines are visited is not defined.
   */
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
