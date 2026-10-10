package com.mentorship.restaurant.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Base for tests that need their own restaurants. Restaurants are inserted straight into the
 * database, so a test only makes the HTTP call it is actually testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public abstract class RestaurantEndpointTestSupport {

  @Autowired protected RestTestClient client;
  @Autowired protected JdbcTemplate jdbcTemplate;

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
    // restaurants, and their menus and items, follow by ON DELETE CASCADE.
    jdbcTemplate.update("DELETE FROM users WHERE user_email LIKE ?", emailPrefix() + "%");
  }

  /** Inserts a user and its restaurant, with no usable password. Returns the restaurant id. */
  protected long insertRestaurant(String name, String email, boolean isOpen) {
    return insertRestaurant(jdbcTemplate, name, email, isOpen);
  }

  /**
   * {@link #insertRestaurant(String, String, boolean)}, static and public so tests on another base
   * (create-order's) can seed a restaurant. Its email must sit under that test's own prefix.
   */
  public static long insertRestaurant(
      JdbcTemplate jdbcTemplate, String name, String email, boolean isOpen) {
    Long restaurantId =
        jdbcTemplate.queryForObject(
            """
            WITH new_user AS (
              INSERT INTO users (user_name, user_email, user_password)
              VALUES (?, ?, '!no-login')
              RETURNING user_id
            )
            INSERT INTO restaurants
              (user_id, restaurant_name, restaurant_description, restaurant_is_open)
            SELECT user_id, ?, 'Test kitchen.', ? FROM new_user
            RETURNING restaurant_id
            """,
            Long.class,
            name,
            email,
            name,
            isOpen);
    if (restaurantId == null) {
      throw new IllegalStateException("Restaurant not created for " + email);
    }
    return restaurantId;
  }

  /**
   * Inserts a menu for the restaurant holding one item, stock 50 at 100.00. Returns the item id.
   * The codes derive from the restaurant id, so call it once per restaurant.
   */
  public static long insertMenuItem(JdbcTemplate jdbcTemplate, long restaurantId) {
    Long menuItemId =
        jdbcTemplate.queryForObject(
            """
            WITH new_menu AS (
              INSERT INTO menus (restaurant_id, menu_code, menu_category)
              VALUES (?, ?, 'Main')
              RETURNING menu_id
            )
            INSERT INTO menu_items
              (menu_id, menu_item_code, menu_item_name, menu_item_price, menu_item_stock)
            SELECT menu_id, ?, 'Test Dish', 100.00, 50 FROM new_menu
            RETURNING menu_item_id
            """,
            Long.class,
            restaurantId,
            "TEST-MENU-" + restaurantId,
            "TEST-ITEM-" + restaurantId);
    if (menuItemId == null) {
      throw new IllegalStateException("Menu item not created for restaurant " + restaurantId);
    }
    return menuItemId;
  }

  protected void softDeleteRestaurant(long restaurantId) {
    softDeleteRestaurant(jdbcTemplate, restaurantId);
  }

  /** {@link #softDeleteRestaurant(long)}, static and public for tests on another base. */
  public static void softDeleteRestaurant(JdbcTemplate jdbcTemplate, long restaurantId) {
    jdbcTemplate.update(
        """
        UPDATE users SET user_deleted_at = now()
        WHERE user_id = (SELECT user_id FROM restaurants WHERE restaurant_id = ?)
        """,
        restaurantId);
  }
}
