package com.mentorship.restaurant.cart;

import com.mentorship.restaurant.support.CartEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ModifyCartItemEndpointTest extends CartEndpointTestSupport {

  @Test
  void modifiesACartItem() {
    long cartId = createCartWithItem(KOFTA, 2);
    long cartItemId = cartItemIdFor(cartId);

    client
        .put()
        .uri("/api/v1/cart/{cartId}/items/{cartItemId}", cartId, cartItemId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {"quantity": 4, "note": "extra sauce"}
            """)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.id")
        .isEqualTo(cartId)
        .jsonPath("$.items.length()")
        .isEqualTo(1)
        .jsonPath("$.items[0].id")
        .isEqualTo(cartItemId)
        .jsonPath("$.items[0].quantity")
        .isEqualTo(4)
        .jsonPath("$.items[0].note")
        .isEqualTo("extra sauce")
        .jsonPath("$.total")
        .isEqualTo(740.00);
  }
}
