package com.mentorship.restaurant.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.AddressEndpointTestSupport;
import org.junit.jupiter.api.Test;

class DeleteAddressEndpointTest extends AddressEndpointTestSupport {

  @Test
  void deletesAnAddress() {
    long addressId = addAddress("Home");

    asCustomer(CUSTOMER_WITHOUT_ADDRESSES)
        .delete()
        .uri("/api/v1/addresses/{addressId}", addressId)
        .exchange()
        .expectStatus()
        .isNoContent();

    assertThat(addressExists(addressId)).isFalse();
  }

  @Test
  void deletingTheDefaultAddressDoesNotPromoteAnotherAddress() {
    long defaultAddressId = insertAddress("Home", true, "2026-01-01T10:00:00Z");
    long otherAddressId = insertAddress("Work", false, "2026-01-02T10:00:00Z");

    asCustomer(CUSTOMER_WITHOUT_ADDRESSES)
        .delete()
        .uri("/api/v1/addresses/{addressId}", defaultAddressId)
        .exchange()
        .expectStatus()
        .isNoContent();

    assertThat(defaultFlagForAddress(defaultAddressId)).isEmpty();
    assertThat(defaultFlagForAddress(otherAddressId)).contains(false);
  }

  @Test
  void rejectsAnUnknownAddress() {
    asCustomer(CUSTOMER_WITHOUT_ADDRESSES)
        .delete()
        .uri("/api/v1/addresses/{addressId}", 999999L)
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

    // Scoped to the caller, someone else's address is not found (#83 part 2).
    asCustomer(OTHER_CUSTOMER)
        .delete()
        .uri("/api/v1/addresses/{addressId}", addressId)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Address not found");

    assertThat(addressExists(addressId)).isTrue();
  }
}
