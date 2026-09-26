package com.mentorship.restaurant.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.CustomerEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class CreateCustomerEndpointTest extends CustomerEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "create.customer.test.";
  }

  private static final String NEW_EMAIL = "create.customer.test.sara@example.com";
  private static final String SEEDED_CUSTOMER_EMAIL = "ahmed.ali@example.com";

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
  void rejectsAPasswordOver72BytesEvenWhenUnder72Characters() {
    // 40 characters, 80 bytes: passes a character count, would be refused by BCrypt.
    String password = "é".repeat(40);

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
                .formatted(NEW_EMAIL, password))
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .value(String.class, message -> assertThat(message).contains("password"));
  }

  @Test
  void allowsReusingTheEmailOfASoftDeletedUser() {
    softDeleteCustomer(insertCustomer(NEW_EMAIL));

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
