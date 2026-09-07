package com.mentorship.restaurant.support;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

// The auto-reject sweep and a suite that seeds PLACED orders with past
// timestamps are in a race the suite would eventually lose. The timer is off;
// the sweep test calls the handler directly, which exercises the same code.
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "app.orders.auto-reject.enabled=false")
@AutoConfigureRestTestClient
@ActiveProfiles({"local", "test"})
public abstract class OrderEndpointTestSupport {
  protected static final long CUSTOMER = 1L;
  protected static final long NILE_KITCHEN = 1L;
  protected static final long BURGER_YARD = 2L;
  protected static final long KOFTA = 1L;

  @Autowired protected RestTestClient client;
  @Autowired protected JdbcTemplate jdbcTemplate;

  private final List<Long> seededOrderIds = new ArrayList<>();

  @BeforeEach
  @AfterEach
  protected void deleteSeededOrders() {
    if (!seededOrderIds.isEmpty()) {
      String placeholders = seededOrderIds.stream().map(id -> "?").collect(Collectors.joining(","));
      jdbcTemplate.update(
          "DELETE FROM orders WHERE order_id IN (" + placeholders + ")", seededOrderIds.toArray());
      seededOrderIds.clear();
    }
    jdbcTemplate.update("UPDATE menu_items SET menu_item_stock = 50 WHERE menu_item_id = ?", KOFTA);
  }

  protected long seedOrder(String status) {
    return seedOrder(status, NILE_KITCHEN);
  }

  protected long seedOrder(String status, long restaurantId) {
    Long orderId =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO orders
              (customer_id, restaurant_id, order_status, order_total, order_created_at)
            VALUES (?, ?, ?, 370.00, ?)
            RETURNING order_id
            """,
            Long.class,
            CUSTOMER,
            restaurantId,
            status,
            OffsetDateTime.now());
    if (orderId == null) {
      throw new IllegalStateException("Order insert returned no id");
    }
    seededOrderIds.add(orderId);
    return orderId;
  }

  /** Seeds an order with an explicit creation time, for the staleness sweep. */
  protected long seedOrderPlacedAt(String status, OffsetDateTime createdAt) {
    Long orderId =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO orders
              (customer_id, restaurant_id, order_status, order_total, order_created_at)
            VALUES (?, ?, ?, 370.00, ?)
            RETURNING order_id
            """,
            Long.class,
            CUSTOMER,
            NILE_KITCHEN,
            status,
            createdAt);
    if (orderId == null) {
      throw new IllegalStateException("Order insert returned no id");
    }
    return orderId;
  }

  protected void seedOrderLine(long orderId, long menuItemId, int quantity) {
    jdbcTemplate.update(
        """
        INSERT INTO order_items
          (order_id, menu_item_id, order_item_name, order_item_quantity, order_item_price)
        VALUES (?, ?, 'Kofta Platter', ?, 185.00)
        """,
        orderId,
        menuItemId,
        quantity);
  }

  protected int stockFor(long menuItemId) {
    Integer stock =
        jdbcTemplate.queryForObject(
            "SELECT menu_item_stock FROM menu_items WHERE menu_item_id = ?",
            Integer.class,
            menuItemId);
    if (stock == null) {
      throw new IllegalStateException("Stock not found for menu item " + menuItemId);
    }
    return stock;
  }

  protected Map<String, Object> orderRow(long orderId) {
    return jdbcTemplate.queryForMap(
        """
        SELECT order_status, order_prep_time_minutes,
               order_rejection_reason, order_rejection_note
        FROM orders WHERE order_id = ?
        """,
        orderId);
  }

  protected String statusOf(long orderId) {
    return jdbcTemplate.queryForObject(
        "SELECT order_status FROM orders WHERE order_id = ?", String.class, orderId);
  }

  protected int historyCountFor(long orderId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM order_status_history WHERE order_id = ?", Integer.class, orderId);
    return count == null ? 0 : count;
  }
}
