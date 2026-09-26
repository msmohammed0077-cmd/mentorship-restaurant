package com.mentorship.restaurant.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.AddressEndpointTestSupport;
import org.junit.jupiter.api.Test;

class DeleteAddressEndpointTest extends AddressEndpointTestSupport {

  @Test
  void deletesAnAddress() {
    long addressId = addAddress("Home");

    client
        .delete()
        .uri(
            "/api/v1/addresses/{addressId}?customerId={customerId}",
            addressId,
            CUSTOMER_WITHOUT_ADDRESSES)
        .exchange()
        .expectStatus()
        .isNoContent();

    assertThat(addressExists(addressId)).isFalse();
  }

  @Test
  void deletingTheDefaultAddressDoesNotPromoteAnotherAddress() {
    long defaultAddressId = insertAddress("Home", true, "2026-01-01T10:00:00Z");
    long otherAddressId = insertAddress("Work", false, "2026-01-02T10:00:00Z");

    client
        .delete()
        .uri(
            "/api/v1/addresses/{addressId}?customerId={customerId}",
            defaultAddressId,
            CUSTOMER_WITHOUT_ADDRESSES)
        .exchange()
        .expectStatus()
        .isNoContent();

    assertThat(defaultFlagForAddress(defaultAddressId)).isEmpty();
    assertThat(defaultFlagForAddress(otherAddressId)).contains(false);
  }

  @Test
  void rejectsAnUnknownAddress() {
    client
        .delete()
        .uri(
            "/api/v1/addresses/{addressId}?customerId={customerId}",
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
        .delete()
        .uri("/api/v1/addresses/{addressId}?customerId={customerId}", addressId, OTHER_CUSTOMER)
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Address belongs to another customer");

    assertThat(addressExists(addressId)).isTrue();
  }
}
