package com.mentorship.restaurant.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.OrderEndpointTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CancelOrderEndpointTest extends OrderEndpointTestSupport {
  private static final long OTHER_CUSTOMER = 2L;
  private static final String CANCEL = "/api/v1/orders/{id}/cancel?customerId={c}&role=CUSTOMER";

  @Test
  void cancelsAPlacedOrder() {
    long orderId = seedOrder("PLACED");

    client
        .post()
        .uri(CANCEL, orderId, CUSTOMER)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.order_id")
        .isEqualTo(orderId)
        .jsonPath("$.status")
        .isEqualTo("CANCELLED");

    assertThat(statusOf(orderId)).isEqualTo("CANCELLED");
  }

  @Test
  void cancellationRestoresStockOnEveryLine() {
    long orderId = seedOrder("PLACED");
    seedOrderLine(orderId, KOFTA, 3);
    int stockBefore = stockFor(KOFTA);

    cancelFor(orderId);

    assertThat(stockFor(KOFTA)).isEqualTo(stockBefore + 3);
  }

  @Test
  void recordsTheCancellationInHistory() {
    long orderId = seedOrder("PLACED");

    cancelFor(orderId);

    Map<String, Object> history =
        jdbcTemplate.queryForMap(
            """
            SELECT order_status_history_from, order_status_history_to,
                   order_status_history_actor_role
            FROM order_status_history WHERE order_id = ?
            """,
            orderId);

    assertThat(history.get("order_status_history_from")).isEqualTo("PLACED");
    assertThat(history.get("order_status_history_to")).isEqualTo("CANCELLED");
    assertThat(history.get("order_status_history_actor_role")).isEqualTo("CUSTOMER");
  }

  @Test
  void refusesAnUnknownOrder() {
    client.post().uri(CANCEL, 999999L, CUSTOMER).exchange().expectStatus().isNotFound();
  }

  @Test
  void refusesAnotherCustomersOrder() {
    long orderId = seedOrder("PLACED");

    client.post().uri(CANCEL, orderId, OTHER_CUSTOMER).exchange().expectStatus().isForbidden();

    assertThat(statusOf(orderId)).isEqualTo("PLACED");
  }

  @Test
  void refusesAcceptedPreparingPickedUpDeliveredAndAlreadyCancelledOrders() {
    for (String status :
        new String[] {"ACCEPTED", "PREPARING", "PICKED_UP", "DELIVERED", "CANCELLED"}) {
      long orderId = seedOrder(status);

      client.post().uri(CANCEL, orderId, CUSTOMER).exchange().expectStatus().isEqualTo(409);

      assertThat(statusOf(orderId)).isEqualTo(status);
      assertThat(historyCountFor(orderId)).isZero();
    }
  }

  @Test
  void refusesRestaurantRole() {
    long orderId = seedOrder("PLACED");

    client
        .post()
        .uri("/api/v1/orders/{id}/cancel?customerId={c}&role=RESTAURANT", orderId, CUSTOMER)
        .exchange()
        .expectStatus()
        .isForbidden();

    assertThat(statusOf(orderId)).isEqualTo("PLACED");
  }

  @Test
  void refusesSystemRole() {
    long orderId = seedOrder("PLACED");

    client
        .post()
        .uri("/api/v1/orders/{id}/cancel?customerId={c}&role=SYSTEM", orderId, CUSTOMER)
        .exchange()
        .expectStatus()
        .isForbidden();

    assertThat(statusOf(orderId)).isEqualTo("PLACED");
  }

  @Test
  void doesNotRestockTwiceWhenCancellationIsRepeated() {
    long orderId = seedOrder("PLACED");
    seedOrderLine(orderId, KOFTA, 3);
    int stockBefore = stockFor(KOFTA);

    cancelFor(orderId);
    int afterFirst = stockFor(KOFTA);

    client.post().uri(CANCEL, orderId, CUSTOMER).exchange().expectStatus().isEqualTo(409);

    assertThat(afterFirst).isEqualTo(stockBefore + 3);
    assertThat(stockFor(KOFTA)).isEqualTo(afterFirst);
  }

  private void cancelFor(long orderId) {
    client.post().uri(CANCEL, orderId, CUSTOMER).exchange().expectStatus().isOk();
  }
}
