package com.mentorship.restaurant.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.AddressEndpointTestSupport;
import org.junit.jupiter.api.Test;

class SetDefaultAddressEndpointTest extends AddressEndpointTestSupport {

  @Test
  void setsTheDefaultAddressAndClearsTheOldOne() {
    long homeAddressId = insertAddress("Home", true, "2026-01-01T10:00:00Z");
    long workAddressId = insertAddress("Work", false, "2026-01-02T10:00:00Z");

    client
        .put()
        .uri(
            "/api/v1/addresses/{addressId}/default?customerId={customerId}",
            workAddressId,
            CUSTOMER_WITHOUT_ADDRESSES)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.id")
        .isEqualTo(workAddressId)
        .jsonPath("$.isDefault")
        .isEqualTo(true);

    assertThat(defaultFlagForAddress(homeAddressId)).contains(false);
    assertThat(defaultFlagForAddress(workAddressId)).contains(true);
  }

  @Test
  void settingTheExistingDefaultIsIdempotent() {
    long addressId = insertAddress("Home", true, "2026-01-01T10:00:00Z");

    client
        .put()
        .uri(
            "/api/v1/addresses/{addressId}/default?customerId={customerId}",
            addressId,
            CUSTOMER_WITHOUT_ADDRESSES)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.id")
        .isEqualTo(addressId)
        .jsonPath("$.isDefault")
        .isEqualTo(true);

    assertThat(defaultFlagForAddress(addressId)).contains(true);
  }

  @Test
  void rejectsAnUnknownAddress() {
    client
        .put()
        .uri(
            "/api/v1/addresses/{addressId}/default?customerId={customerId}",
            999999L,
            CUSTOMER_WITHOUT_ADDRESSES)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Address not found");
  }

  @Test
  void rejectsAnAddressBelongingToAnotherCustomer() {
    long addressId = addAddress("Home");

    client
        .put()
        .uri(
            "/api/v1/addresses/{addressId}/default?customerId={customerId}",
            addressId,
            OTHER_CUSTOMER)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Address not found");
  }
}
