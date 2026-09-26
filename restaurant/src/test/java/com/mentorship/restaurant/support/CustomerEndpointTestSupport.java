package com.mentorship.restaurant.support;

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
}
