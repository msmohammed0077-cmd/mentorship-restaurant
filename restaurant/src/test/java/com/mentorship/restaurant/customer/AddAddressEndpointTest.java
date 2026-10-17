package com.mentorship.restaurant.customer;

import com.mentorship.restaurant.support.AddressEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AddAddressEndpointTest extends AddressEndpointTestSupport {

  @Test
  void addsTheFirstAddressAsDefault() {
    asCustomer(CUSTOMER_WITHOUT_ADDRESSES)
        .post()
        .uri("/api/v1/addresses")
        .contentType(MediaType.APPLICATION_JSON)
        .body(addressBody("Home"))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.customer_id")
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
        .jsonPath("$.is_default")
        .isEqualTo(true);
  }

  @Test
  void addsLaterAddressesWithoutMakingThemDefault() {
    addAddress("Home");

    asCustomer(CUSTOMER_WITHOUT_ADDRESSES)
        .post()
        .uri("/api/v1/addresses")
        .contentType(MediaType.APPLICATION_JSON)
        .body(addressBody("Work"))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.label")
        .isEqualTo("Work")
        .jsonPath("$.is_default")
        .isEqualTo(false);
  }

  @Test
  void rejectsBlankRequiredFields() {
    asCustomer(CUSTOMER_WITHOUT_ADDRESSES)
        .post()
        .uri("/api/v1/addresses")
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
}
