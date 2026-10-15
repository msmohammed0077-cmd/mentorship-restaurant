package com.mentorship.restaurant.exception;

import com.mentorship.restaurant.support.CartEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ErrorResponseEndpointTest extends CartEndpointTestSupport {

  @Test
  void reportsEveryInvalidField() {
    asCustomer(CUSTOMER_WITHOUT_CART)
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body("{}")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .value(
            String.class,
            message ->
                org.assertj.core.api.Assertions.assertThat(message)
                    .contains("menuItemId")
                    .contains("quantity"));
  }

  @Test
  void rejectsAMalformedCartId() {
    asCustomer(CUSTOMER_WITHOUT_CART)
        .get()
        .uri("/api/v1/cart/{cartId}", "not-a-number")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("cartId is not a valid value");
  }

  @Test
  void rejectsAMissingCartItemIdsParameter() {
    long cartId = createCartWithItem(KOFTA, 1);

    asCustomer(CUSTOMER_WITHOUT_CART)
        .delete()
        .uri("/api/v1/cart/{cartId}/items", cartId)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("cartItemIds is required");
  }
}
