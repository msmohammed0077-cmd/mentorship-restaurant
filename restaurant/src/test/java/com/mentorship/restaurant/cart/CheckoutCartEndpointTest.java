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
}
