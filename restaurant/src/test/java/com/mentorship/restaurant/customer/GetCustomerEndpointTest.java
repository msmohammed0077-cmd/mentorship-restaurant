package com.mentorship.restaurant.customer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class GetCustomerEndpointTest {

  // Every user this class creates has an email under this prefix, so cleanup can never reach a
  // seeded user.
  private static final String TEST_EMAIL_PREFIX = "get.customer.test.";
  private static final String DELETED_EMAIL = TEST_EMAIL_PREFIX + "sara@example.com";

  @Autowired private RestTestClient client;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  @AfterEach
  void deleteCreatedUsers() {
    // customers rows follow by ON DELETE CASCADE.
    jdbcTemplate.update("DELETE FROM users WHERE user_email LIKE ?", TEST_EMAIL_PREFIX + "%");
  }

  @Test
  void returnsASeededCustomerWithoutThePassword() {
    client
        .get()
        .uri("/api/v1/customers/1")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.customer_id")
        .isEqualTo(1)
        .jsonPath("$.name")
        .isEqualTo("Ahmed Ali")
        .jsonPath("$.email")
        .isEqualTo("ahmed.ali@example.com")
        .jsonPath("$.password")
        .doesNotExist();
  }

  @Test
  void rejectsAnUnknownCustomer() {
    client
        .get()
        .uri("/api/v1/customers/999999")
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Customer not found");
  }

  @Test
  void rejectsASoftDeletedCustomer() {
    client
        .post()
        .uri("/api/v1/customers")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {
              "name": "Sara Youssef",
              "email": "%s",
              "password": "s3cret-pass"
            }
            """
                .formatted(DELETED_EMAIL))
        .exchange()
        .expectStatus()
        .isCreated();
    jdbcTemplate.update(
        "UPDATE users SET user_deleted_at = now() WHERE user_email = ?", DELETED_EMAIL);
    Long customerId =
        jdbcTemplate.queryForObject(
            """
            SELECT c.customer_id FROM customers c JOIN users u ON u.user_id = c.user_id
            WHERE u.user_email = ?
            """,
            Long.class,
            DELETED_EMAIL);

    client
        .get()
        .uri("/api/v1/customers/{customerId}", customerId)
        .exchange()
        .expectStatus()
        .isNotFound();
  }
}
