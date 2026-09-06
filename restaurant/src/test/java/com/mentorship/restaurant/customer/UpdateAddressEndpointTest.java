package com.mentorship.restaurant.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.AddressEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class UpdateAddressEndpointTest extends AddressEndpointTestSupport {

  @Test
  void updatesAnAddressWithoutChangingDefaultState() {
    long addressId = insertAddress("Home", true, "2026-01-01T10:00:00Z");

    client
        .put()
        .uri(
            "/api/v1/addresses/{addressId}?customerId={customerId}",
            addressId,
            CUSTOMER_WITHOUT_ADDRESSES)
        .contentType(MediaType.APPLICATION_JSON)
        .body(updatedAddressBody("Parents"))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.id")
        .isEqualTo(addressId)
        .jsonPath("$.label")
        .isEqualTo("Parents")
        .jsonPath("$.line")
        .isEqualTo("45 Ramses Street")
        .jsonPath("$.city")
        .isEqualTo("Giza")
        .jsonPath("$.area")
        .isEqualTo("Mohandessin")
        .jsonPath("$.note")
        .isEqualTo("Ring twice")
        .jsonPath("$.isDefault")
        .isEqualTo(true);

    assertThat(defaultFlagForAddress(addressId)).contains(true);
  }

  @Test
  void rejectsAnUnknownAddress() {
    client
        .put()
        .uri(
            "/api/v1/addresses/{addressId}?customerId={customerId}",
            999999L,
            CUSTOMER_WITHOUT_ADDRESSES)
        .contentType(MediaType.APPLICATION_JSON)
        .body(updatedAddressBody("Parents"))
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
        .uri("/api/v1/addresses/{addressId}?customerId={customerId}", addressId, OTHER_CUSTOMER)
        .contentType(MediaType.APPLICATION_JSON)
        .body(updatedAddressBody("Parents"))
        .exchange()
        .expectStatus()
        .isForbidden()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Address belongs to another customer");
  }

  @Test
  void rejectsBlankRequiredFields() {
    long addressId = addAddress("Home");

    client
        .put()
        .uri(
            "/api/v1/addresses/{addressId}?customerId={customerId}",
            addressId,
            CUSTOMER_WITHOUT_ADDRESSES)
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {
              "label": "",
              "line": "",
              "city": "",
              "area": ""
            }
            """)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .value(
            String.class,
            message ->
                assertThat(message)
                    .contains("label")
                    .contains("line")
                    .contains("city")
                    .contains("area"));
  }
}
