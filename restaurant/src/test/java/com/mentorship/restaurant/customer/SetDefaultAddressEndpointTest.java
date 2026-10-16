package com.mentorship.restaurant.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.AddressEndpointTestSupport;
import org.junit.jupiter.api.Test;

class SetDefaultAddressEndpointTest extends AddressEndpointTestSupport {

  @Test
  void setsTheDefaultAddressAndClearsTheOldOne() {
    long homeAddressId = insertAddress("Home", true, "2026-01-01T10:00:00Z");
    long workAddressId = insertAddress("Work", false, "2026-01-02T10:00:00Z");

    asCustomer(CUSTOMER_WITHOUT_ADDRESSES)
        .put()
        .uri("/api/v1/addresses/{addressId}/default", workAddressId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.id")
        .isEqualTo(workAddressId)
        .jsonPath("$.is_default")
        .isEqualTo(true);

    assertThat(defaultFlagForAddress(homeAddressId)).contains(false);
    assertThat(defaultFlagForAddress(workAddressId)).contains(true);
  }

  @Test
  void settingTheExistingDefaultIsIdempotent() {
    long addressId = insertAddress("Home", true, "2026-01-01T10:00:00Z");

    asCustomer(CUSTOMER_WITHOUT_ADDRESSES)
        .put()
        .uri("/api/v1/addresses/{addressId}/default", addressId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.id")
        .isEqualTo(addressId)
        .jsonPath("$.is_default")
        .isEqualTo(true);

    assertThat(defaultFlagForAddress(addressId)).contains(true);
  }

  @Test
  void rejectsAnUnknownAddress() {
    asCustomer(CUSTOMER_WITHOUT_ADDRESSES)
        .put()
        .uri("/api/v1/addresses/{addressId}/default", 999999L)
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

    asCustomer(OTHER_CUSTOMER)
        .put()
        .uri("/api/v1/addresses/{addressId}/default", addressId)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Address not found");
  }
}
