package com.mentorship.restaurant.support;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public abstract class OrderEndpointTestSupport {
  protected static final long CUSTOMER = 1L;
  protected static final long NILE_KITCHEN = 1L;
  protected static final long BURGER_YARD = 2L;
  protected static final long KOFTA = 1L;

  @Autowired protected RestTestClient client;
  @Autowired protected JdbcTemplate jdbcTemplate;

  @BeforeEach
  @AfterEach
  protected void deleteSeededOrders() {
    jdbcTemplate.update("DELETE FROM orders");
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
    return orderId;
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
