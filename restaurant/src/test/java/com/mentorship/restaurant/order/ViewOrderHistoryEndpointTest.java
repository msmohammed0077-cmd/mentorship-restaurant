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

    asCustomer(CUSTOMER)
        .get()
        .uri("/api/v1/orders")
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

    asCustomer(CUSTOMER)
        .get()
        .uri("/api/v1/orders")
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

    asCustomer(CUSTOMER)
        .get()
        .uri("/api/v1/orders?limit=2")
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

    asCustomer(CUSTOMER)
        .get()
        .uri(
            "/api/v1/orders?limit=2"
                + "&cursor.createdAt={ts}&cursor.orderId={cid}",
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

    asCustomer(CUSTOMER)
        .get()
        .uri("/api/v1/orders?limit=1")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.orders[0].order_id")
        .isEqualTo(higher);

    String[] c = readCursor(1);
    String cursorTs = c[0];
    String cursorId = c[1];

    asCustomer(CUSTOMER)
        .get()
        .uri(
            "/api/v1/orders?limit=1"
                + "&cursor.createdAt={ts}&cursor.orderId={cid}",
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

    asCustomer(CUSTOMER)
        .get()
        .uri("/api/v1/orders?limit=1")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.orders[0].order_id")
        .isEqualTo(newer);

    String[] c = readCursor(1);
    String cursorTs = c[0];
    String cursorId = c[1];

    asCustomer(CUSTOMER)
        .get()
        .uri(
            "/api/v1/orders?limit=1"
                + "&cursor.createdAt={ts}&cursor.orderId={cid}",
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
    asCustomer(CUSTOMER)
        .get()
        .uri("/api/v1/orders")
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
  void rejectsAnEmptyLimitRatherThanFailing() {
    asCustomer(CUSTOMER)
        .get()
        .uri("/api/v1/orders?limit=")
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void rejectsACursorWhoseTimestampIsOutOfRange() {
    asCustomer(CUSTOMER)
        .get()
        .uri(
            "/api/v1/orders"
                + "?cursor.createdAt=999999999-01-01T00:00:00Z&cursor.orderId=1")
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void rejectsAHalfSuppliedCursor() {
    asCustomer(CUSTOMER)
        .get()
        .uri("/api/v1/orders?cursor.orderId=1")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Cursor needs both createdAt and orderId");
  }

  @Test
  void rejectsALimitBelowOne() {
    asCustomer(CUSTOMER)
        .get()
        .uri("/api/v1/orders?limit=0")
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void rejectsALimitAboveFifty() {
    asCustomer(CUSTOMER)
        .get()
        .uri("/api/v1/orders?limit=51")
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void rejectsAnUnparseableCursorTimestamp() {
    asCustomer(CUSTOMER)
        .get()
        .uri(
            "/api/v1/orders"
                + "?cursor.createdAt=not-a-date&cursor.orderId=1")
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void refusesARestaurantOwner() {
    asRestaurantOwner(NILE_KITCHEN)
        .get()
        .uri("/api/v1/orders")
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Caller is not a customer");
  }

  private String[] readCursor(int limit) {
    byte[] body =
        asCustomer(CUSTOMER)
            .get()
            .uri("/api/v1/orders?limit={limit}", limit)
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
