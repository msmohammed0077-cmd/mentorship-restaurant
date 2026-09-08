package com.mentorship.restaurant.order;

import com.mentorship.restaurant.support.OrderEndpointTestSupport;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ViewOrderHistoryEndpointTest extends OrderEndpointTestSupport {
  private static final long OTHER_CUSTOMER = 2L;

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
        .uri("/api/v1/orders?customerId={id}&role=CUSTOMER", CUSTOMER)
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
        .uri("/api/v1/orders?customerId={id}&role=CUSTOMER", CUSTOMER)
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
        .uri("/api/v1/orders?customerId={id}&limit=2&role=CUSTOMER", CUSTOMER)
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

    String[] c = readCursor(2);
    String cursorTs = c[0];
    String cursorId = c[1];

    client
        .get()
        .uri(
            "/api/v1/orders?customerId={id}&limit=2&role=CUSTOMER"
                + "&cursor.createdAt={ts}&cursor.orderId={cid}",
            CUSTOMER,
            cursorTs,
            cursorId)
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
        .uri("/api/v1/orders?customerId={id}&limit=1&role=CUSTOMER", CUSTOMER)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.orders[0].order_id")
        .isEqualTo(higher);

    String[] c = readCursor(1);
    String cursorTs = c[0];
    String cursorId = c[1];

    client
        .get()
        .uri(
            "/api/v1/orders?customerId={id}&limit=1&role=CUSTOMER"
                + "&cursor.createdAt={ts}&cursor.orderId={cid}",
            CUSTOMER,
            cursorTs,
            cursorId)
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
  void doesNotSkipOrdersSharingAMillisecond() {
    OffsetDateTime base = OffsetDateTime.of(2026, 9, 5, 9, 0, 0, 123_456_000, ZoneOffset.UTC);
    long newer = seedOrderFor(CUSTOMER, base.withNano(123_500_000));
    long older = seedOrderFor(CUSTOMER, base);

    client
        .get()
        .uri("/api/v1/orders?customerId={id}&role=CUSTOMER&limit=1", CUSTOMER)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.orders[0].order_id")
        .isEqualTo(newer);

    String[] c = readCursor(1);
    String cursorTs = c[0];
    String cursorId = c[1];

    client
        .get()
        .uri(
            "/api/v1/orders?customerId={id}&role=CUSTOMER&limit=1"
                + "&cursor.createdAt={ts}&cursor.orderId={cid}",
            CUSTOMER,
            cursorTs,
            cursorId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.orders.length()")
        .isEqualTo(1)
        .jsonPath("$.orders[0].order_id")
        .isEqualTo(older);
  }

  @Test
  void returnsAnEmptyPageForACustomerWithNoOrders() {
    client
        .get()
        .uri("/api/v1/orders?customerId={id}&role=CUSTOMER", CUSTOMER)
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
  void rejectsARoleThatIsNotACustomer() {
    client
        .get()
        .uri("/api/v1/orders?customerId={id}&role=RESTAURANT", CUSTOMER)
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void rejectsAMissingRole() {
    client
        .get()
        .uri("/api/v1/orders?customerId={id}", CUSTOMER)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void rejectsAnEmptyLimitRatherThanFailing() {
    client
        .get()
        .uri("/api/v1/orders?customerId={id}&role=CUSTOMER&limit=", CUSTOMER)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void rejectsACursorWhoseTimestampIsOutOfRange() {
    client
        .get()
        .uri(
            "/api/v1/orders?customerId={id}&role=CUSTOMER"
                + "&cursor.createdAt=999999999-01-01T00:00:00Z&cursor.orderId=1",
            CUSTOMER)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void rejectsAHalfSuppliedCursor() {
    client
        .get()
        .uri("/api/v1/orders?customerId={id}&role=CUSTOMER&cursor.orderId=1", CUSTOMER)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Cursor needs both createdAt and orderId");
  }

  @Test
  void rejectsAnUnknownCustomer() {
    client
        .get()
        .uri("/api/v1/orders?customerId={id}&role=CUSTOMER", 999999L)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Customer not found");
  }

  @Test
  void rejectsAMissingCustomerId() {
    client.get().uri("/api/v1/orders?role=CUSTOMER").exchange().expectStatus().isBadRequest();
  }

  @Test
  void rejectsALimitBelowOne() {
    client
        .get()
        .uri("/api/v1/orders?customerId={id}&limit=0&role=CUSTOMER", CUSTOMER)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void rejectsALimitAboveFifty() {
    client
        .get()
        .uri("/api/v1/orders?customerId={id}&limit=51&role=CUSTOMER", CUSTOMER)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void rejectsAnUnparseableCursorTimestamp() {
    client
        .get()
        .uri(
            "/api/v1/orders?customerId={id}&role=CUSTOMER"
                + "&cursor.createdAt=not-a-date&cursor.orderId=1",
            CUSTOMER)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  private String[] readCursor(int limit) {
    byte[] body =
        client
            .get()
            .uri("/api/v1/orders?customerId={id}&role=CUSTOMER&limit={limit}", CUSTOMER, limit)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .returnResult()
            .getResponseBody();
    String json = new String(body, StandardCharsets.UTF_8);
    Matcher cursor = Pattern.compile("\"next_cursor\":\\{([^}]*)\\}").matcher(json);
    if (!cursor.find()) {
      throw new IllegalStateException("No next_cursor in " + json);
    }
    String fields = cursor.group(1);
    Matcher ts = Pattern.compile("\"created_at\":\"([^\"]+)\"").matcher(fields);
    Matcher id = Pattern.compile("\"order_id\":(\\d+)").matcher(fields);
    if (!ts.find() || !id.find()) {
      throw new IllegalStateException("Incomplete next_cursor in " + json);
    }
    return new String[] {ts.group(1), id.group(1)};
  }
}
