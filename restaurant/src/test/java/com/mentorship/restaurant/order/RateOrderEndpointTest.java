package com.mentorship.restaurant.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.OrderEndpointTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class RateOrderEndpointTest extends OrderEndpointTestSupport {
  private static final long OTHER_CUSTOMER = 2L;
  private static final String RATE = "/api/v1/orders/{id}/rating";

  @Test
  void ratesADeliveredOrder() {
    long orderId = seedOrder("DELIVERED");

    client
        .post()
        .uri(RATE, orderId)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"customer_id\": 1, \"score\": 5, \"comment\": \"Excellent handoff\"}")
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.rating_id")
        .exists()
        .jsonPath("$.order_id")
        .isEqualTo(orderId)
        .jsonPath("$.score")
        .isEqualTo(5)
        .jsonPath("$.comment")
        .isEqualTo("Excellent handoff")
        .jsonPath("$.created_at")
        .exists();

    Map<String, Object> rating =
        jdbcTemplate.queryForMap(
            """
            SELECT order_id, order_rating_score, order_rating_comment
            FROM order_ratings WHERE order_id = ?
            """,
            orderId);

    assertThat(rating.get("order_id")).isEqualTo(orderId);
    assertThat(rating.get("order_rating_score")).isEqualTo(5);
    assertThat(rating.get("order_rating_comment")).isEqualTo("Excellent handoff");
  }

  @Test
  void ratesADeliveredOrderWithoutAComment() {
    long orderId = seedOrder("DELIVERED");

    client
        .post()
        .uri(RATE, orderId)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"customer_id\": 1, \"score\": 4}")
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.comment")
        .isEmpty();

    assertThat(ratingCountFor(orderId)).isEqualTo(1);
  }

  @Test
  void refusesAnUnknownOrder() {
    client
        .post()
        .uri(RATE, 999999L)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"customer_id\": 1, \"score\": 5}")
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void refusesAnOrderThatIsNotDelivered() {
    for (String status :
        new String[] {"PLACED", "ACCEPTED", "REJECTED", "PREPARING", "PICKED_UP", "CANCELLED"}) {
      long orderId = seedOrder(status);

      client
          .post()
          .uri(RATE, orderId)
          .contentType(MediaType.APPLICATION_JSON)
          .body("{\"customer_id\": 1, \"score\": 5}")
          .exchange()
          .expectStatus()
          .isEqualTo(409)
          .expectBody()
          .jsonPath("$.message")
          .isEqualTo("Order is not delivered");

      assertThat(ratingCountFor(orderId)).isZero();
    }
  }

  @Test
  void refusesAnAlreadyRatedOrder() {
    long orderId = seedOrder("DELIVERED");
    seedRating(orderId, 4);

    client
        .post()
        .uri(RATE, orderId)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"customer_id\": 1, \"score\": 5}")
        .exchange()
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Order is already rated");

    assertThat(ratingCountFor(orderId)).isEqualTo(1);
  }

  @Test
  void refusesAnotherCustomersOrder() {
    long orderId = seedOrder("DELIVERED");

    client
        .post()
        .uri(RATE, orderId)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"customer_id\": %d, \"score\": 5}".formatted(OTHER_CUSTOMER))
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Order belongs to another customer");

    assertThat(ratingCountFor(orderId)).isZero();
  }

  @Test
  void refusesAScoreBelowTheAllowedRange() {
    long orderId = seedOrder("DELIVERED");

    client
        .post()
        .uri(RATE, orderId)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"customer_id\": 1, \"score\": 0}")
        .exchange()
        .expectStatus()
        .isBadRequest();

    assertThat(ratingCountFor(orderId)).isZero();
  }

  @Test
  void refusesAScoreAboveTheAllowedRange() {
    long orderId = seedOrder("DELIVERED");

    client
        .post()
        .uri(RATE, orderId)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"customer_id\": 1, \"score\": 6}")
        .exchange()
        .expectStatus()
        .isBadRequest();

    assertThat(ratingCountFor(orderId)).isZero();
  }
}
