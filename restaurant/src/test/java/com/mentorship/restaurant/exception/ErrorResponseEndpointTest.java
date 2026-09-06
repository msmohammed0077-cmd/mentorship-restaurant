package com.mentorship.restaurant.exception;

import com.mentorship.restaurant.support.CartEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ErrorResponseEndpointTest extends CartEndpointTestSupport {

  /** Field error order is not deterministic, so this asserts on containment, not equality. */
  @Test
  void reportsEveryInvalidField() {
    client
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
                    .contains("customerId")
                    .contains("menuItemId")
                    .contains("quantity"));
  }

  /**
   * An unhandled exception's own message can carry SQL or class names, so the response says only
   * that something went wrong.
   *
   * <p>That this path is a 500 at all is a separate bug: Spring MVC's own
   * MethodArgumentTypeMismatchException should be a 400, and will be once the advice extends
   * ResponseEntityExceptionHandler. The assertion here is about what the body may disclose.
   */
  @Test
  void hidesInternalDetailFromUnexpectedErrors() {
    client
        .get()
        .uri("/api/v1/cart/{cartId}", "not-a-number")
        .exchange()
        .expectStatus()
        .is5xxServerError()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("An unexpected error occurred");
  }
}
