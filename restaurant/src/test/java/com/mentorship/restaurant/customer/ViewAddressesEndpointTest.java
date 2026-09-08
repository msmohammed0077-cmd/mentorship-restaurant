package com.mentorship.restaurant.customer;

import com.mentorship.restaurant.support.AddressEndpointTestSupport;
import org.junit.jupiter.api.Test;

class ViewAddressesEndpointTest extends AddressEndpointTestSupport {

  @Test
  void listsAddressesWithDefaultFirstThenNewest() {
    insertAddress("Home", true, "2026-01-01T10:00:00Z");
    insertAddress("Work", false, "2026-01-02T10:00:00Z");
    insertAddress("Gym", false, "2026-01-03T10:00:00Z");

    client
        .get()
        .uri("/api/v1/addresses?customerId={customerId}", CUSTOMER_WITHOUT_ADDRESSES)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.length()")
        .isEqualTo(3)
        .jsonPath("$[0].label")
        .isEqualTo("Home")
        .jsonPath("$[0].isDefault")
        .isEqualTo(true)
        .jsonPath("$[1].label")
        .isEqualTo("Gym")
        .jsonPath("$[2].label")
        .isEqualTo("Work");
  }

  @Test
  void returnsAnEmptyListForACustomerWithoutAddresses() {
    client
        .get()
        .uri("/api/v1/addresses?customerId={customerId}", CUSTOMER_WITHOUT_ADDRESSES)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.length()")
        .isEqualTo(0);
  }

  @Test
  void rejectsAnUnknownCustomer() {
    client
        .get()
        .uri("/api/v1/addresses?customerId={customerId}", 999999L)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Customer not found");
  }
}
