package com.mentorship.restaurant.cart;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.CartEndpointTestSupport;
import org.junit.jupiter.api.Test;

class ClearCartEndpointTest extends CartEndpointTestSupport {

  private static final long SEEDED_CART = 1L;

  @Test
  void clearsACart() {
    long cartId = createCartWithItem(KOFTA, 2);

    asCustomer(CUSTOMER_WITHOUT_CART)
        .delete()
        .uri("/api/v1/cart/{cartId}", cartId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.id")
        .isEqualTo(cartId)
        .jsonPath("$.customer_id")
        .isEqualTo(CUSTOMER_WITHOUT_CART)
        .jsonPath("$.items.length()")
        .isEqualTo(0)
        .jsonPath("$.total")
        .isEqualTo(0);

    assertThat(cartItemCountFor(cartId)).isZero();
    assertThat(cartExists(cartId)).isTrue();
  }

  @Test
  void clearsOnlyTheTargetCart() {
    long cartId = createCartWithItem(KOFTA, 2);
    int otherCartItemCount = cartItemCountFor(SEEDED_CART);

    emptyCart(cartId);

    assertThat(cartItemCountFor(SEEDED_CART)).isEqualTo(otherCartItemCount);
  }

  @Test
  void clearsAnAlreadyEmptyCart() {
    long cartId = createCartWithItem(KOFTA, 2);
    emptyCart(cartId);

    asCustomer(CUSTOMER_WITHOUT_CART)
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
    asCustomer(CUSTOMER_WITHOUT_CART)
        .delete()
        .uri("/api/v1/cart/{cartId}", 999999L)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Cart not found");
  }

  @Test
  void rejectsAnotherCustomersCart() {
    long cartId = createCartWithItem(KOFTA, 1);

    // Scoped to the caller, someone else's cart is not found (#83 part 2).
    asCustomer(1L)
        .delete()
        .uri("/api/v1/cart/{cartId}", cartId)
        .exchange()
        .expectStatus()
        .isNotFound();

    assertThat(cartItemCountFor(cartId)).isEqualTo(1);
  }
}
