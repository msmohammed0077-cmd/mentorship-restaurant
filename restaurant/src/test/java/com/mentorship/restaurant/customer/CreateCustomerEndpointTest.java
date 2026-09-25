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
class CreateCustomerEndpointTest {

  // Every user this class creates has an email under this prefix, so cleanup can never reach a
  // seeded user.
  private static final String TEST_EMAIL_PREFIX = "create.customer.test.";
  private static final String NEW_EMAIL = TEST_EMAIL_PREFIX + "sara@example.com";
  private static final String SEEDED_CUSTOMER_EMAIL = "ahmed.ali@example.com";

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
  void createsACustomerAndNeverReturnsThePassword() {
    client
        .post()
        .uri("/api/v1/customers")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {
              "name": "Sara Youssef",
              "email": "Create.Customer.Test.SARA@Example.com",
              "password": "s3cret-pass",
              "phone": "+201001234567",
              "date_of_birth": "1995-04-12",
              "gender": "FEMALE"
            }
            """)
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.customer_id")
        .isNotEmpty()
        .jsonPath("$.name")
        .isEqualTo("Sara Youssef")
        .jsonPath("$.email")
        .isEqualTo(NEW_EMAIL)
        .jsonPath("$.phone")
        .isEqualTo("+201001234567")
        .jsonPath("$.date_of_birth")
        .isEqualTo("1995-04-12")
        .jsonPath("$.gender")
        .isEqualTo("FEMALE")
        .jsonPath("$.password")
        .doesNotExist();

    String storedPassword =
        jdbcTemplate.queryForObject(
            "SELECT user_password FROM users WHERE user_email = ?", String.class, NEW_EMAIL);
    assertThat(storedPassword).isNotEqualTo("s3cret-pass");
    assertThat(passwordEncoder.matches("s3cret-pass", storedPassword)).isTrue();
  }

  @Test
  void rejectsAnEmailAlreadyUsedByAnActiveUser() {
    createCustomer(SEEDED_CUSTOMER_EMAIL).expectStatus().isEqualTo(409);
    createCustomer(SEEDED_CUSTOMER_EMAIL.toUpperCase())
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Email is already in use");
  }

  @Test
  void rejectsAnInvalidBody() {
    client
        .post()
        .uri("/api/v1/customers")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {
              "name": "Sara Youssef",
              "password": "short"
            }
            """)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .value(String.class, message -> assertThat(message).contains("email").contains("password"));
  }

  @Test
  void allowsReusingTheEmailOfASoftDeletedUser() {
    createCustomer(NEW_EMAIL).expectStatus().isCreated();
    jdbcTemplate.update("UPDATE users SET user_deleted_at = now() WHERE user_email = ?", NEW_EMAIL);

    createCustomer(NEW_EMAIL).expectStatus().isCreated();
  }

  private RestTestClient.ResponseSpec createCustomer(String email) {
    return client
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
                .formatted(email))
        .exchange();
  }
}
