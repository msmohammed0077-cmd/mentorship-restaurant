package com.mentorship.restaurant.customer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ChangePasswordEndpointTest {

  // Every user this class creates has an email under this prefix, so cleanup can never reach a
  // seeded user.
  private static final String TEST_EMAIL_PREFIX = "change.password.test.";
  private static final String CUSTOMER_EMAIL = TEST_EMAIL_PREFIX + "sara@example.com";
  private static final String CURRENT_PASSWORD = "s3cret-pass";

  @Autowired private RestTestClient client;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private PasswordEncoder passwordEncoder;

  @BeforeEach
  @AfterEach
  void deleteCreatedUsers() {
    // customers rows follow by ON DELETE CASCADE.
    jdbcTemplate.update("DELETE FROM users WHERE user_email LIKE ?", TEST_EMAIL_PREFIX + "%");
  }

  @Test
  void changesThePasswordWhenTheCurrentOneMatches() {
    Long customerId = createCustomer();

    changePassword(customerId, CURRENT_PASSWORD, "n3w-secret-pass").expectStatus().isNoContent();

    String storedPassword =
        jdbcTemplate.queryForObject(
            "SELECT user_password FROM users WHERE user_email = ?", String.class, CUSTOMER_EMAIL);
    assertThat(passwordEncoder.matches("n3w-secret-pass", storedPassword)).isTrue();
  }

  @Test
  void rejectsAWrongCurrentPassword() {
    Long customerId = createCustomer();

    changePassword(customerId, "wrong-pass", "n3w-secret-pass")
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Current password is incorrect");
  }

  private RestTestClient.ResponseSpec changePassword(
      Long customerId, String currentPassword, String newPassword) {
    return client
        .put()
        .uri("/api/v1/customers/{customerId}/password", customerId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {
              "current_password": "%s",
              "new_password": "%s"
            }
            """
                .formatted(currentPassword, newPassword))
        .exchange();
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
              "password": "%s"
            }
            """
                .formatted(CUSTOMER_EMAIL, CURRENT_PASSWORD))
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
