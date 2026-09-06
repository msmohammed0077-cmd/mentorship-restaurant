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

  /**
   * Stock belongs to add, modify and checkout. A cart whose item has since outrun its stock is
   * still a cart the customer is entitled to look at, so this reads back rather than failing.
   */
  @Test
  void viewsACartWhoseQuantityExceedsCurrentStock() {
    long cartId = createCartWithItem(KOFTA, 40);
    setStock(KOFTA, 1);

    client
        .get()
        .uri("/api/v1/cart/{cartId}", cartId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.items[0].quantity")
        .isEqualTo(40);
  }

  @Test
  void viewsAnEmptyCart() {
    long cartId = createCartWithItem(KOFTA, 2);
    client.delete().uri("/api/v1/cart/{cartId}", cartId).exchange().expectStatus().isOk();

    client
        .get()
        .uri("/api/v1/cart/{cartId}", cartId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.items.length()")
        .isEqualTo(0)
        .jsonPath("$.total")
        .isEqualTo(0);
  }

  @Test
  void rejectsAnUnknownCart() {
    client
        .get()
        .uri("/api/v1/cart/{cartId}", 999999L)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Cart not found");
  }
}
