package com.mentorship.restaurant.customer;

import com.mentorship.restaurant.support.CustomerEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class UpdateCustomerEndpointTest extends CustomerEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "update.customer.test.";
  }

  @Test
  void updatesNameAndPhoneAndLeavesTheRestUnchanged() {
    long customerId = insertCustomer(email("sara"));

    asCustomer(customerId)
        .patch()
        .uri("/api/v1/customers/me")
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

  /** Keycloak owns the login email (#83 part 3): the profile update no longer changes it. */
  @Test
  void leavesTheEmailUnchanged() {
    long customerId = insertCustomer(email("sara"));

    asCustomer(customerId)
        .patch()
        .uri("/api/v1/customers/me")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            { "email": "%s" }
            """
                .formatted(email("other")))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.email")
        .isEqualTo(email("sara"));
  }
}
