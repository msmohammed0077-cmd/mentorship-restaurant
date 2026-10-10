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

  protected void softDeleteRestaurant(long restaurantId) {
    jdbcTemplate.update(
        """
        UPDATE users SET user_deleted_at = now()
        WHERE user_id = (SELECT user_id FROM restaurants WHERE restaurant_id = ?)
        """,
        restaurantId);
  }
}
