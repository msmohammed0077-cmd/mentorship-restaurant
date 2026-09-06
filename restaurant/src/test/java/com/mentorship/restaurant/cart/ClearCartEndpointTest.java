package com.mentorship.restaurant.cart;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.CartEndpointTestSupport;
import org.junit.jupiter.api.Test;

class ClearCartEndpointTest extends CartEndpointTestSupport {

  private static final long SEEDED_CART = 1L;

  @Test
  void clearsACart() {
    long cartId = createCartWithItem(KOFTA, 2);

    client
        .delete()
        .uri("/api/v1/cart/{cartId}", cartId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.id")
        .isEqualTo(cartId)
        .jsonPath("$.customerId")
        .isEqualTo(CUSTOMER_WITHOUT_CART)
        .jsonPath("$.items.length()")
        .isEqualTo(0)
        .jsonPath("$.total")
        .isEqualTo(0);

    // The response is built from the entity, which can read empty while the rows survive.
    assertThat(cartItemCountFor(cartId)).isZero();
    assertThat(cartExists(cartId)).isTrue();
  }

  @Test
  void clearsOnlyTheTargetCart() {
    long cartId = createCartWithItem(KOFTA, 2);
    int otherCartItemCount = cartItemCountFor(SEEDED_CART);

    client.delete().uri("/api/v1/cart/{cartId}", cartId).exchange().expectStatus().isOk();

    assertThat(cartItemCountFor(SEEDED_CART)).isEqualTo(otherCartItemCount);
  }

  @Test
  void clearsAnAlreadyEmptyCart() {
    long cartId = createCartWithItem(KOFTA, 2);
    client.delete().uri("/api/v1/cart/{cartId}", cartId).exchange().expectStatus().isOk();

    client
        .delete()
        .uri("/api/v1/cart/{cartId}", cartId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.items.length()")
        .isEqualTo(0);
  }

  @Test
  void rejectsAnUnknownCart() {
    client
        .delete()
        .uri("/api/v1/cart/{cartId}", 999999L)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Cart not found");
  }
}
