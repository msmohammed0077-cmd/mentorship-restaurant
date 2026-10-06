package com.mentorship.restaurant.support;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Base for tests that need their own customers. Customers are inserted straight into the database,
 * so a test only makes the HTTP call it is actually testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public abstract class CustomerEndpointTestSupport {

  protected static final String TEST_PASSWORD = "s3cret-pass";
  protected static final long NILE_KITCHEN = 1L;

  @Autowired protected RestTestClient client;
  @Autowired protected JdbcTemplate jdbcTemplate;
  @Autowired protected PasswordEncoder passwordEncoder;

  /**
   * Every user a test class creates has an email under this prefix, so cleanup can never reach a
   * seeded user. Must be unique per class.
   */
  protected abstract String emailPrefix();

  protected String email(String name) {
    return emailPrefix() + name + "@example.com";
  }

  @BeforeEach
  @AfterEach
  protected void deleteCreatedUsers() {
    // customers, and every row hanging off them, follow by ON DELETE CASCADE.
    jdbcTemplate.update("DELETE FROM users WHERE user_email LIKE ?", emailPrefix() + "%");
  }

  /** Inserts a user and its customer, password {@link #TEST_PASSWORD}. Returns the customer id. */
  protected long insertCustomer(String email) {
    Long customerId =
        jdbcTemplate.queryForObject(
            """
            WITH new_user AS (
              INSERT INTO users (
                user_name, user_email, user_password, user_phone, user_date_of_birth, user_gender
              )
              VALUES ('Sara Youssef', ?, ?, '+201001234567', DATE '1995-04-12', 'FEMALE')
              RETURNING user_id
            )
            INSERT INTO customers (user_id) SELECT user_id FROM new_user
            RETURNING customer_id
            """,
            Long.class,
            email,
            passwordEncoder.encode(TEST_PASSWORD));
    if (customerId == null) {
      throw new IllegalStateException("Customer not created for " + email);
    }
    return customerId;
  }

  protected void softDeleteCustomer(long customerId) {
    jdbcTemplate.update(
        """
        UPDATE users SET user_deleted_at = now()
        WHERE user_id = (SELECT user_id FROM customers WHERE customer_id = ?)
        """,
        customerId);
  }

  /** Inserts an order at Nile Kitchen for the customer. Returns the order id. */
  protected long insertOrder(long customerId, String status) {
    Long orderId =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO orders
              (customer_id, restaurant_id, address_id, order_status, order_total, order_created_at)
            VALUES (?, ?, ?, ?, 370.00, ?)
            RETURNING order_id
            """,
            Long.class,
            customerId,
            NILE_KITCHEN,
            addressIdForCustomer(jdbcTemplate, customerId),
            status,
            OffsetDateTime.now());
    if (orderId == null) {
      throw new IllegalStateException("Order not created for customer " + customerId);
    }
    return orderId;
  }

  /**
   * The customer's default address, else their newest, else a new one. Static so {@link
   * OrderEndpointTestSupport}, which does not extend this class, can share it.
   */
  protected static long addressIdForCustomer(JdbcTemplate jdbcTemplate, long customerId) {
    List<Long> existing =
        jdbcTemplate.queryForList(
            """
            SELECT address_id FROM addresses WHERE customer_id = ?
            ORDER BY address_is_default DESC, address_created_at DESC LIMIT 1
            """,
            Long.class,
            customerId);
    if (!existing.isEmpty()) {
      return existing.get(0);
    }
    Long addressId =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO addresses (
              customer_id, address_label, address_line, address_city, address_area,
              address_note, address_is_default
            )
            VALUES (?, 'Home', '12 Tahrir Street', 'Cairo', 'Dokki', 'Blue gate', TRUE)
            RETURNING address_id
            """,
            Long.class,
            customerId);
    if (addressId == null) {
      throw new IllegalStateException("Address not created for customer " + customerId);
    }
    return addressId;
  }

  /** Inserts an empty cart for the customer. Returns the cart id. */
  protected long insertCart(long customerId) {
    Long cartId =
        jdbcTemplate.queryForObject(
            "INSERT INTO carts (customer_id) VALUES (?) RETURNING cart_id", Long.class, customerId);
    if (cartId == null) {
      throw new IllegalStateException("Cart not created for customer " + customerId);
    }
    return cartId;
  }

  /** Adds a line at the menu item's current price, as add-to-cart would. */
  protected void insertCartItem(long cartId, long menuItemId, int quantity) {
    jdbcTemplate.update(
        """
        INSERT INTO cart_items (cart_id, menu_item_id, cart_item_quantity, cart_item_price)
        SELECT ?, menu_item_id, ?, menu_item_price FROM menu_items WHERE menu_item_id = ?
        """,
        cartId,
        quantity,
        menuItemId);
  }

  protected boolean cartExists(long cartId) {
    Boolean exists =
        jdbcTemplate.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM carts WHERE cart_id = ?)", Boolean.class, cartId);
    return Boolean.TRUE.equals(exists);
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

  protected int orderCountFor(long customerId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM orders WHERE customer_id = ?", Integer.class, customerId);
    return count == null ? 0 : count;
  }

  protected Optional<Map<String, Object>> transactionFor(long orderId) {
    return jdbcTemplate
        .queryForList("SELECT * FROM transaction WHERE order_id = ?", orderId)
        .stream()
        .findFirst();
  }
}
