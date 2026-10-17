package com.mentorship.restaurant.customer;

import com.mentorship.restaurant.support.CustomerEndpointTestSupport;
import org.junit.jupiter.api.Test;

class GetCustomerEndpointTest extends CustomerEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "get.customer.test.";
  }

  @Test
  void returnsTheCallersOwnProfile() {
    asCustomer(1L)
        .get()
        .uri("/api/v1/customers/me")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.customer_id")
        .isEqualTo(1)
        .jsonPath("$.name")
        .isEqualTo("Ahmed Ali")
        .jsonPath("$.email")
        .isEqualTo("ahmed.ali@example.com");
  }

  @Test
  void rejectsASoftDeletedCustomer() {
    long customerId = insertCustomer(email("sara"));
    softDeleteCustomer(customerId);

    asCustomer(customerId).get().uri("/api/v1/customers/me").exchange().expectStatus().isNotFound();
  }
}
