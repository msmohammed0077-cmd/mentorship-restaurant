package com.mentorship.restaurant.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.OrderEndpointTestSupport;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class AcceptRejectOrderEndpointTest extends OrderEndpointTestSupport {

  @Autowired private com.mentorship.restaurant.order.service.OrderService orderService;

  private static final String ACCEPT =
      "/api/v1/orders/{id}/accept?restaurantId={r}&role=RESTAURANT";
  private static final String REJECT =
      "/api/v1/orders/{id}/reject?restaurantId={r}&role=RESTAURANT";

  @Test
  void acceptsAPlacedOrderWithoutTouchingStock() {
    long orderId = seedOrder("PLACED");
    seedOrderLine(orderId, KOFTA, 2);
    int stockBefore = stockFor(KOFTA);

    client
        .post()
        .uri(ACCEPT, orderId, NILE_KITCHEN)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("ACCEPTED");

    assertThat(statusOf(orderId)).isEqualTo("ACCEPTED");
    assertThat(stockFor(KOFTA)).isEqualTo(stockBefore);
    assertThat(historyCountFor(orderId)).isEqualTo(1);
  }

  @Test
  void storesAPrepTimeWhenOneIsGiven() {
    long orderId = seedOrder("PLACED");

    client
        .post()
        .uri(ACCEPT, orderId, NILE_KITCHEN)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"prep_time_minutes\": 25}")
        .exchange()
        .expectStatus()
        .isOk();

    assertThat(orderRow(orderId).get("order_prep_time_minutes")).isEqualTo(25);
  }

  @Test
  void acceptsWithoutABody() {
    long orderId = seedOrder("PLACED");

    client.post().uri(ACCEPT, orderId, NILE_KITCHEN).exchange().expectStatus().isOk();

    assertThat(orderRow(orderId).get("order_prep_time_minutes")).isNull();
  }

  @Test
  void rejectsAPlacedOrderAndStoresTheReason() {
    long orderId = seedOrder("PLACED");

    client
        .post()
        .uri(REJECT, orderId, NILE_KITCHEN)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"reason\": \"TOO_BUSY\", \"note\": \"Kitchen is backed up\"}")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo("REJECTED");

    Map<String, Object> order = orderRow(orderId);
    assertThat(order.get("order_rejection_reason")).isEqualTo("TOO_BUSY");
    assertThat(order.get("order_rejection_note")).isEqualTo("Kitchen is backed up");
    assertThat(historyCountFor(orderId)).isEqualTo(1);
  }

  @Test
  void rejectionRestoresStockOnEveryLine() {
    long orderId = seedOrder("PLACED");
    seedOrderLine(orderId, KOFTA, 3);
    int stockBefore = stockFor(KOFTA);

    rejectFor(orderId, "OUT_OF_INGREDIENTS");

    assertThat(stockFor(KOFTA)).isEqualTo(stockBefore + 3);
  }

  @Test
  void refusesToAcceptAnAlreadyAcceptedOrderAndLeavesStockAlone() {
    long orderId = seedOrder("ACCEPTED");
    seedOrderLine(orderId, KOFTA, 2);
    int stockBefore = stockFor(KOFTA);

    client.post().uri(ACCEPT, orderId, NILE_KITCHEN).exchange().expectStatus().isEqualTo(409);

    assertThat(stockFor(KOFTA)).isEqualTo(stockBefore);
  }

  @Test
  void doesNotRestockTwiceWhenRejectionIsRepeated() {
    long orderId = seedOrder("PLACED");
    seedOrderLine(orderId, KOFTA, 3);
    int stockBefore = stockFor(KOFTA);

    rejectFor(orderId, "TOO_BUSY");
    int afterFirst = stockFor(KOFTA);

    client
        .post()
        .uri(REJECT, orderId, NILE_KITCHEN)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"reason\": \"TOO_BUSY\"}")
        .exchange()
        .expectStatus()
        .isEqualTo(409);

    // A double restock is silent, permanent, and invisible until inventory drifts.
    assertThat(afterFirst).isEqualTo(stockBefore + 3);
    assertThat(stockFor(KOFTA)).isEqualTo(afterFirst);
  }

  @Test
  void refusesToRejectAfterAccepting() {
    long orderId = seedOrder("ACCEPTED");

    client
        .post()
        .uri(REJECT, orderId, NILE_KITCHEN)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"reason\": \"TOO_BUSY\"}")
        .exchange()
        .expectStatus()
        .isEqualTo(409);
  }

  @Test
  void refusesAReasonOutsideTheSet() {
    long orderId = seedOrder("PLACED");

    client
        .post()
        .uri(REJECT, orderId, NILE_KITCHEN)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"reason\": \"BECAUSE\"}")
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void refusesAMissingReason() {
    long orderId = seedOrder("PLACED");

    client
        .post()
        .uri(REJECT, orderId, NILE_KITCHEN)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"note\": \"no reason given\"}")
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void refusesARestaurantUsingTheSystemOnlyReason() {
    long orderId = seedOrder("PLACED");

    client
        .post()
        .uri(REJECT, orderId, NILE_KITCHEN)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"reason\": \"NO_RESPONSE\"}")
        .exchange()
        .expectStatus()
        .isBadRequest();

    assertThat(statusOf(orderId)).isEqualTo("PLACED");
  }

  @Test
  void refusesANonPositivePrepTime() {
    long orderId = seedOrder("PLACED");

    client
        .post()
        .uri(ACCEPT, orderId, NILE_KITCHEN)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"prep_time_minutes\": 0}")
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void refusesAnotherRestaurantsOrder() {
    long orderId = seedOrder("PLACED", NILE_KITCHEN);

    client.post().uri(ACCEPT, orderId, BURGER_YARD).exchange().expectStatus().isForbidden();
  }

  @Test
  void refusesACustomerAccepting() {
    long orderId = seedOrder("PLACED");

    client
        .post()
        .uri("/api/v1/orders/{id}/accept?restaurantId={r}&role=CUSTOMER", orderId, NILE_KITCHEN)
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void refusesAnUnknownOrder() {
    client.post().uri(ACCEPT, 999999L, NILE_KITCHEN).exchange().expectStatus().isNotFound();
  }

  @Test
  void autoRejectsAnOrderLeftPlacedPastTheDeadline() {
    long orderId = seedOrderPlacedAt("PLACED", OffsetDateTime.now().minusMinutes(20));
    seedOrderLine(orderId, KOFTA, 2);
    int stockBefore = stockFor(KOFTA);

    orderService.autoRejectStaleOrders();

    Map<String, Object> order = orderRow(orderId);
    assertThat(order.get("order_status")).isEqualTo("REJECTED");
    assertThat(order.get("order_rejection_reason")).isEqualTo("NO_RESPONSE");
    assertThat(stockFor(KOFTA)).isEqualTo(stockBefore + 2);

    String actor =
        jdbcTemplate.queryForObject(
            "SELECT order_status_history_actor_role FROM order_status_history WHERE order_id = ?",
            String.class,
            orderId);
    assertThat(actor).isEqualTo("SYSTEM");
  }

  @Test
  void leavesAnOrderInsideTheDeadlineAlone() {
    long orderId = seedOrderPlacedAt("PLACED", OffsetDateTime.now().minusMinutes(5));

    orderService.autoRejectStaleOrders();

    assertThat(statusOf(orderId)).isEqualTo("PLACED");
  }

  @Test
  void refusesACallerSupplyingTheSystemRole() {
    long orderId = seedOrder("PLACED", NILE_KITCHEN);

    // SYSTEM skips the ownership check by design. Accepting it over HTTP let
    // anyone reject any restaurant's order, defeating both the 403 ownership
    // check and the 400 reserved-reason guard at once.
    client
        .post()
        .uri("/api/v1/orders/{id}/reject?restaurantId={r}&role=SYSTEM", orderId, 999L)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"reason\": \"NO_RESPONSE\"}")
        .exchange()
        .expectStatus()
        .isForbidden();

    assertThat(statusOf(orderId)).isEqualTo("PLACED");
  }

  @Test
  void refusesACallerSupplyingTheSystemRoleOnAccept() {
    long orderId = seedOrder("PLACED", NILE_KITCHEN);

    client
        .post()
        .uri("/api/v1/orders/{id}/accept?restaurantId={r}&role=SYSTEM", orderId, 999L)
        .exchange()
        .expectStatus()
        .isForbidden();

    assertThat(statusOf(orderId)).isEqualTo("PLACED");
  }

  private void rejectFor(long orderId, String reason) {
    client
        .post()
        .uri(REJECT, orderId, NILE_KITCHEN)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"reason\": \"%s\"}".formatted(reason))
        .exchange()
        .expectStatus()
        .isOk();
  }
}
