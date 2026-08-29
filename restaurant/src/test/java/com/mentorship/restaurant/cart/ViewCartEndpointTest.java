package com.mentorship.restaurant.cart;

import com.mentorship.restaurant.support.CartEndpointTestSupport;
import org.junit.jupiter.api.Test;

class ViewCartEndpointTest extends CartEndpointTestSupport {

  @Test
  void viewsACart() {
    long cartId = createCartWithItem(KOFTA, 2);

    client
        .get()
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
        .isEqualTo(1)
        .jsonPath("$.items[0].itemName")
        .isEqualTo("Kofta Platter")
        .jsonPath("$.items[0].quantity")
        .isEqualTo(2)
        .jsonPath("$.total")
        .isEqualTo(370.00);
  }
}
