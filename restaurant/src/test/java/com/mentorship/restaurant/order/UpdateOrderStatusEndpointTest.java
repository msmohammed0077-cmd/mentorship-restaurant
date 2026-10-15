package com.mentorship.restaurant.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.OrderEndpointTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UpdateOrderStatusEndpointTest extends OrderEndpointTestSupport {
  @Test
  void movesAnAcceptedOrderToPreparing() {
    long orderId = seedOrder("ACCEPTED");

    asRestaurantOwner(NILE_KITCHEN)
        .post()
        .uri(
            "/api/v1/orders/{id}/preparing?restaurantId={r}", orderId, NILE_KITCHEN)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.order_id")
        .isEqualTo(orderId)
        .jsonPath("$.status")
        .isEqualTo("PREPARING");

    assertThat(statusOf(orderId)).isEqualTo("PREPARING");
  }

  @Test
  void recordsTheTransitionInHistory() {
    long orderId = seedOrder("ACCEPTED");

    asRestaurantOwner(NILE_KITCHEN)
        .post()
        .uri(
            "/api/v1/orders/{id}/preparing?restaurantId={r}", orderId, NILE_KITCHEN)
        .exchange()
        .expectStatus()
        .isOk();

    Map<String, Object> history =
        jdbcTemplate.queryForMap(
            """
            SELECT order_status_history_from, order_status_history_to,
                   order_status_history_actor_role
            FROM order_status_history WHERE order_id = ?
            """,
            orderId);

    assertThat(history.get("order_status_history_from")).isEqualTo("ACCEPTED");
    assertThat(history.get("order_status_history_to")).isEqualTo("PREPARING");
    assertThat(history.get("order_status_history_actor_role")).isEqualTo("RESTAURANT");
  }

  @Test
  void movesAPreparingOrderToReadyForPickup() {
    long orderId = seedOrder("PREPARING");

    asRestaurantOwner(NILE_KITCHEN)
        .post()
        .uri(
            "/api/v1/orders/{id}/ready-for-pickup?restaurantId={r}", orderId, NILE_KITCHEN)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("READY_FOR_PICKUP");
  }

  @Test
  void refusesToSkipAStep() {
    long orderId = seedOrder("PLACED");

    asRestaurantOwner(NILE_KITCHEN)
        .post()
        .uri(
            "/api/v1/orders/{id}/preparing?restaurantId={r}", orderId, NILE_KITCHEN)
        .exchange()
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Order is not in status ACCEPTED");

    assertThat(statusOf(orderId)).isEqualTo("PLACED");
  }

  @Test
  void refusesARepeatOfTheCurrentStatus() {
    long orderId = seedOrder("PREPARING");

    asRestaurantOwner(NILE_KITCHEN)
        .post()
        .uri(
            "/api/v1/orders/{id}/preparing?restaurantId={r}", orderId, NILE_KITCHEN)
        .exchange()
        .expectStatus()
        .isEqualTo(409);
  }

  @Test
  void refusesToMoveATerminalOrder() {
    long orderId = seedOrder("DELIVERED");

    asRestaurantOwner(NILE_KITCHEN)
        .post()
        .uri(
            "/api/v1/orders/{id}/preparing?restaurantId={r}", orderId, NILE_KITCHEN)
        .exchange()
        .expectStatus()
        .isEqualTo(409);

    assertThat(statusOf(orderId)).isEqualTo("DELIVERED");
  }

  @Test
  void writesNoHistoryRowForARefusedTransition() {
    long orderId = seedOrder("PLACED");

    asRestaurantOwner(NILE_KITCHEN)
        .post()
        .uri(
            "/api/v1/orders/{id}/preparing?restaurantId={r}", orderId, NILE_KITCHEN)
        .exchange()
        .expectStatus()
        .isEqualTo(409);

    assertThat(historyCountFor(orderId)).isZero();
  }

  @Test
  void refusesAnotherRestaurantsOrder() {
    long orderId = seedOrder("ACCEPTED", NILE_KITCHEN);

    asRestaurantOwner(BURGER_YARD)
        .post()
        .uri("/api/v1/orders/{id}/preparing?restaurantId={r}", orderId, BURGER_YARD)
        .exchange()
        .expectStatus()
        .isForbidden();

    assertThat(statusOf(orderId)).isEqualTo("ACCEPTED");
  }

  @Test
  void refusesARestaurantTheCallerDoesNotOwn() {
    long orderId = seedOrder("ACCEPTED");

    asRestaurantOwner(BURGER_YARD)
        .post()
        .uri("/api/v1/orders/{id}/preparing?restaurantId={r}", orderId, NILE_KITCHEN)
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant belongs to another owner");

    assertThat(statusOf(orderId)).isEqualTo("ACCEPTED");
  }

  @Test
  void refusesAnUnknownOrder() {
    asRestaurantOwner(NILE_KITCHEN)
        .post()
        .uri(
            "/api/v1/orders/{id}/preparing?restaurantId={r}", 999999L, NILE_KITCHEN)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Order not found");
  }
}
