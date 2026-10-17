package com.mentorship.restaurant.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.OrderEndpointTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CancelOrderEndpointTest extends OrderEndpointTestSupport {
  private static final long OTHER_CUSTOMER = 2L;
  private static final String CANCEL = "/api/v1/orders/{id}/cancel";

  @Test
  void cancelsAPlacedOrder() {
    long orderId = seedOrder("PLACED");

    asCustomer(CUSTOMER)
        .post()
        .uri(CANCEL, orderId)
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
    asCustomer(CUSTOMER).post().uri(CANCEL, 999999L).exchange().expectStatus().isNotFound();
  }

  @Test
  void refusesAnotherCustomersOrder() {
    long orderId = seedOrder("PLACED");

    // Scoped to the caller, someone else's order is not found (#83 part 2).
    asCustomer(OTHER_CUSTOMER).post().uri(CANCEL, orderId).exchange().expectStatus().isNotFound();

    assertThat(statusOf(orderId)).isEqualTo("PLACED");
  }

  @Test
  void refusesAcceptedPreparingPickedUpDeliveredAndAlreadyCancelledOrders() {
    for (String status :
        new String[] {"ACCEPTED", "PREPARING", "PICKED_UP", "DELIVERED", "CANCELLED"}) {
      long orderId = seedOrder(status);

      asCustomer(CUSTOMER).post().uri(CANCEL, orderId).exchange().expectStatus().isEqualTo(409);

      assertThat(statusOf(orderId)).isEqualTo(status);
      assertThat(historyCountFor(orderId)).isZero();
    }
  }

  @Test
  void doesNotRestockTwiceWhenCancellationIsRepeated() {
    long orderId = seedOrder("PLACED");
    seedOrderLine(orderId, KOFTA, 3);
    int stockBefore = stockFor(KOFTA);

    cancelFor(orderId);
    int afterFirst = stockFor(KOFTA);

    asCustomer(CUSTOMER).post().uri(CANCEL, orderId).exchange().expectStatus().isEqualTo(409);

    assertThat(afterFirst).isEqualTo(stockBefore + 3);
    assertThat(stockFor(KOFTA)).isEqualTo(afterFirst);
  }

  private void cancelFor(long orderId) {
    asCustomer(CUSTOMER).post().uri(CANCEL, orderId).exchange().expectStatus().isOk();
  }
}
