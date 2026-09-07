package com.mentorship.restaurant.order;

import com.mentorship.restaurant.support.OrderEndpointTestSupport;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ViewOrderHistoryEndpointTest extends OrderEndpointTestSupport {

  /** Customer 2 is user 2, which also holds orders.read_order. */
  private static final long OTHER_CUSTOMER = 2L;

  /** Customer 3 is user 5, absent from app.permissions, so it holds nothing. */
  private static final long CUSTOMER_WITHOUT_PERMISSION = 3L;

  private static final OffsetDateTime EARLY =
      OffsetDateTime.of(2026, 9, 1, 9, 0, 0, 0, ZoneOffset.UTC);
  private static final OffsetDateTime LATE =
      OffsetDateTime.of(2026, 9, 5, 9, 0, 0, 0, ZoneOffset.UTC);

  @Test
  void returnsTheCustomersOrdersNewestFirst() {
    long older = seedOrderFor(CUSTOMER, EARLY);
    seedOrderLine(older, KOFTA, 2);
    long newer = seedOrderFor(CUSTOMER, LATE);
    seedOrderLine(newer, KOFTA, 2);

    client
        .get()
        .uri("/api/v1/orders?customerId={id}", CUSTOMER)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.orders.length()")
        .isEqualTo(2)
        .jsonPath("$.orders[0].order_id")
        .isEqualTo(newer)
        .jsonPath("$.orders[0].status")
        .isEqualTo("PLACED")
        .jsonPath("$.orders[0].restaurant_name")
        .isEqualTo("Nile Kitchen")
        .jsonPath("$.orders[0].item_count")
        .isEqualTo(1)
        .jsonPath("$.orders[1].order_id")
        .isEqualTo(older)
        .jsonPath("$.next_cursor")
        .doesNotExist();
  }

  @Test
  void showsNoOtherCustomersOrders() {
    seedOrderFor(OTHER_CUSTOMER, LATE);

    client
        .get()
        .uri("/api/v1/orders?customerId={id}", CUSTOMER)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.orders.length()")
        .isEqualTo(0);
  }

  @Test
  void followsTheCursorWithNoOverlapAndNoGap() {
    long first = seedOrderFor(CUSTOMER, LATE);
    long second = seedOrderFor(CUSTOMER, EARLY.plusDays(2));
    long third = seedOrderFor(CUSTOMER, EARLY);

    client
        .get()
        .uri("/api/v1/orders?customerId={id}&limit=2", CUSTOMER)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.orders.length()")
        .isEqualTo(2)
        .jsonPath("$.orders[0].order_id")
        .isEqualTo(first)
        .jsonPath("$.orders[1].order_id")
        .isEqualTo(second)
        .jsonPath("$.next_cursor")
        .exists();

    String cursor = readCursor(2);

    client
        .get()
        .uri("/api/v1/orders?customerId={id}&limit=2&cursor={cursor}", CUSTOMER, cursor)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.orders.length()")
        .isEqualTo(1)
        .jsonPath("$.orders[0].order_id")
        .isEqualTo(third)
        .jsonPath("$.next_cursor")
        .doesNotExist();
  }

  @Test
  void breaksATimestampTieByOrderIdAcrossThePageBoundary() {
    long lower = seedOrderFor(CUSTOMER, LATE);
    long higher = seedOrderFor(CUSTOMER, LATE);

    client
        .get()
        .uri("/api/v1/orders?customerId={id}&limit=1", CUSTOMER)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.orders[0].order_id")
        .isEqualTo(higher);

    String cursor = readCursor(1);

    client
        .get()
        .uri("/api/v1/orders?customerId={id}&limit=1&cursor={cursor}", CUSTOMER, cursor)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.orders.length()")
        .isEqualTo(1)
        .jsonPath("$.orders[0].order_id")
        .isEqualTo(lower);
  }

  @Test
  void returnsAnEmptyPageForACustomerWithNoOrders() {
    client
        .get()
        .uri("/api/v1/orders?customerId={id}", CUSTOMER)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.orders.length()")
        .isEqualTo(0)
        .jsonPath("$.next_cursor")
        .doesNotExist();
  }

  @Test
  void rejectsACustomerWhoseUserHoldsNoPermission() {
    client
        .get()
        .uri("/api/v1/orders?customerId={id}", CUSTOMER_WITHOUT_PERMISSION)
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Missing permission: orders.read_order");
  }

  @Test
  void rejectsAnUnknownCustomer() {
    client
        .get()
        .uri("/api/v1/orders?customerId={id}", 999999L)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Customer not found");
  }

  @Test
  void rejectsAMissingCustomerId() {
    client.get().uri("/api/v1/orders").exchange().expectStatus().isBadRequest();
  }

  @Test
  void rejectsALimitBelowOne() {
    client
        .get()
        .uri("/api/v1/orders?customerId={id}&limit=0", CUSTOMER)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void rejectsALimitAboveFifty() {
    client
        .get()
        .uri("/api/v1/orders?customerId={id}&limit=51", CUSTOMER)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void rejectsAMalformedCursor() {
    client
        .get()
        .uri("/api/v1/orders?customerId={id}&cursor=not-a-cursor", CUSTOMER)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Invalid cursor");
  }

  /**
   * Reads next_cursor out of a first page. Issues the request a second time on purpose:
   * RestTestClient's fluent assertions consume the response, and a duplicate GET reads more clearly
   * than threading the body through the assertion chain. The endpoint is a read.
   */
  private String readCursor(int limit) {
    byte[] body =
        client
            .get()
            .uri("/api/v1/orders?customerId={id}&limit={limit}", CUSTOMER, limit)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .returnResult()
            .getResponseBody();
    String json = new String(body, StandardCharsets.UTF_8);
    Matcher matcher = Pattern.compile("\"next_cursor\":\"([^\"]+)\"").matcher(json);
    if (!matcher.find()) {
      throw new IllegalStateException("No next_cursor in " + json);
    }
    return matcher.group(1);
  }
}
