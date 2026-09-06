package com.mentorship.restaurant.customer;

import com.mentorship.restaurant.support.AddressEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AddAddressEndpointTest extends AddressEndpointTestSupport {

  @Test
  void addsTheFirstAddressAsDefault() {
    client
        .post()
        .uri("/api/v1/addresses?customerId={customerId}", CUSTOMER_WITHOUT_ADDRESSES)
        .contentType(MediaType.APPLICATION_JSON)
        .body(addressBody("Home"))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.customerId")
        .isEqualTo(CUSTOMER_WITHOUT_ADDRESSES)
        .jsonPath("$.label")
        .isEqualTo("Home")
        .jsonPath("$.line")
        .isEqualTo("12 Tahrir Street")
        .jsonPath("$.city")
        .isEqualTo("Cairo")
        .jsonPath("$.area")
        .isEqualTo("Dokki")
        .jsonPath("$.note")
        .isEqualTo("Blue gate")
        .jsonPath("$.isDefault")
        .isEqualTo(true);
  }

  @Test
  void addsLaterAddressesWithoutMakingThemDefault() {
    addAddress("Home");

    client
        .post()
        .uri("/api/v1/addresses?customerId={customerId}", CUSTOMER_WITHOUT_ADDRESSES)
        .contentType(MediaType.APPLICATION_JSON)
        .body(addressBody("Work"))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.label")
        .isEqualTo("Work")
        .jsonPath("$.isDefault")
        .isEqualTo(false);
  }

  @Test
  void rejectsAnUnknownCustomer() {
    client
        .post()
        .uri("/api/v1/addresses?customerId={customerId}", 999999L)
        .contentType(MediaType.APPLICATION_JSON)
        .body(addressBody("Home"))
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Customer not found");
  }

  @Test
  void rejectsBlankRequiredFields() {
    client
        .post()
        .uri("/api/v1/addresses?customerId={customerId}", CUSTOMER_WITHOUT_ADDRESSES)
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
                org.assertj.core.api.Assertions.assertThat(message)
                    .contains("label")
                    .contains("line")
                    .contains("city")
                    .contains("area"));
  }

  @Test
  void rejectsMissingCustomerId() {
    client
        .post()
        .uri("/api/v1/addresses")
        .contentType(MediaType.APPLICATION_JSON)
        .body(addressBody("Home"))
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("customerId is required");
  }
}
