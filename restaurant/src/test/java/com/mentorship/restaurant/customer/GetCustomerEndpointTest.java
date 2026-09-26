package com.mentorship.restaurant.customer;

import com.mentorship.restaurant.support.CustomerEndpointTestSupport;
import org.junit.jupiter.api.Test;

class GetCustomerEndpointTest extends CustomerEndpointTestSupport {

  @Override
  protected String emailPrefix() {
    return "get.customer.test.";
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
    long customerId = insertCustomer(email("sara"));
    softDeleteCustomer(customerId);

    client
        .get()
        .uri("/api/v1/customers/{customerId}", customerId)
        .exchange()
        .expectStatus()
        .isNotFound();
  }
}
