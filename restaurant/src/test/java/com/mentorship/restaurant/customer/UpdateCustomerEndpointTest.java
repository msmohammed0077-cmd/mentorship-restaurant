package com.mentorship.restaurant.customer;

import com.mentorship.restaurant.support.CustomerEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class UpdateCustomerEndpointTest extends CustomerEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "update.customer.test.";
  }

  private static final String SEEDED_CUSTOMER_EMAIL = "ahmed.ali@example.com";

  @Test
  void updatesNameAndPhoneAndLeavesTheRestUnchanged() {
    long customerId = insertCustomer(email("sara"));

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
        .isEqualTo((int) customerId)
        .jsonPath("$.name")
        .isEqualTo("Sara Hassan")
        .jsonPath("$.phone")
        .isEqualTo("+201009876543")
        .jsonPath("$.email")
        .isEqualTo(email("sara"))
        .jsonPath("$.date_of_birth")
        .isEqualTo("1995-04-12")
        .jsonPath("$.gender")
        .isEqualTo("FEMALE");
  }

  @Test
  void rejectsAnEmailUsedByAnotherActiveUser() {
    long customerId = insertCustomer(email("sara"));

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
    long customerId = insertCustomer(email("sara"));

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
}
