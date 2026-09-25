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
class UpdateCustomerEndpointTest {

  // Every user this class creates has an email under this prefix, so cleanup can never reach a
  // seeded user.
  private static final String TEST_EMAIL_PREFIX = "update.customer.test.";
  private static final String CUSTOMER_EMAIL = TEST_EMAIL_PREFIX + "sara@example.com";
  private static final String SEEDED_CUSTOMER_EMAIL = "ahmed.ali@example.com";

  @Autowired private RestTestClient client;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  @AfterEach
  void deleteCreatedUsers() {
    // customers rows follow by ON DELETE CASCADE.
    jdbcTemplate.update("DELETE FROM users WHERE user_email LIKE ?", TEST_EMAIL_PREFIX + "%");
  }

  @Test
  void updatesNameAndPhoneAndLeavesTheRestUnchanged() {
    Long customerId = createCustomer();

    client
        .patch()
        .uri("/api/v1/customers/{customerId}", customerId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {
              "name": "Sara Hassan",
              "phone": "+201009876543"
            }
            """)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.customer_id")
        .isEqualTo(customerId.intValue())
        .jsonPath("$.name")
        .isEqualTo("Sara Hassan")
        .jsonPath("$.phone")
        .isEqualTo("+201009876543")
        .jsonPath("$.email")
        .isEqualTo(CUSTOMER_EMAIL)
        .jsonPath("$.date_of_birth")
        .isEqualTo("1995-04-12")
        .jsonPath("$.gender")
        .isEqualTo("FEMALE");
  }

  @Test
  void rejectsAnEmailUsedByAnotherActiveUser() {
    Long customerId = createCustomer();

    client
        .patch()
        .uri("/api/v1/customers/{customerId}", customerId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            { "email": "%s" }
            """
                .formatted(SEEDED_CUSTOMER_EMAIL))
        .exchange()
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Email is already in use");
  }

  @Test
  void rejectsAnUnknownCustomer() {
    client
        .patch()
        .uri("/api/v1/customers/999999")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            { "name": "Sara Hassan" }
            """)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Customer not found");
  }

  @Test
  void rejectsAnInvalidEmail() {
    Long customerId = createCustomer();

    client
        .patch()
        .uri("/api/v1/customers/{customerId}", customerId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            { "email": "not-an-email" }
            """)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  private Long createCustomer() {
    client
        .post()
        .uri("/api/v1/customers")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {
              "name": "Sara Youssef",
              "email": "%s",
              "password": "s3cret-pass",
              "phone": "+201001234567",
              "date_of_birth": "1995-04-12",
              "gender": "FEMALE"
            }
            """
                .formatted(CUSTOMER_EMAIL))
        .exchange()
        .expectStatus()
        .isCreated();
    return jdbcTemplate.queryForObject(
        """
        SELECT c.customer_id FROM customers c JOIN users u ON u.user_id = c.user_id
        WHERE u.user_email = ?
        """,
        Long.class,
        CUSTOMER_EMAIL);
  }
}
