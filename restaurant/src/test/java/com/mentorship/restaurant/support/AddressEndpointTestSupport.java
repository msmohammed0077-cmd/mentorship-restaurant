package com.mentorship.restaurant.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public abstract class AddressEndpointTestSupport {

  protected static final long CUSTOMER_WITHOUT_ADDRESSES = 3L;

  @Autowired protected RestTestClient client;
  @Autowired protected JdbcTemplate jdbcTemplate;

  @BeforeEach
  @AfterEach
  protected void resetAddressFixtures() {
    jdbcTemplate.update("DELETE FROM addresses WHERE customer_id = ?", CUSTOMER_WITHOUT_ADDRESSES);
  }

  protected String addressBody(String label) {
    return """
        {
          "label": "%s",
          "line": "12 Tahrir Street",
          "city": "Cairo",
          "area": "Dokki",
          "note": "Blue gate"
        }
        """
        .formatted(label);
  }

  protected long addAddress(String label) {
    client
        .post()
        .uri("/api/v1/addresses?customerId={customerId}", CUSTOMER_WITHOUT_ADDRESSES)
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .body(addressBody(label))
        .exchange()
        .expectStatus()
        .isCreated();

    return addressIdForLabel(label);
  }

  protected long addressIdForLabel(String label) {
    Long addressId =
        jdbcTemplate.queryForObject(
            "SELECT address_id FROM addresses WHERE customer_id = ? AND address_label = ?",
            Long.class,
            CUSTOMER_WITHOUT_ADDRESSES,
            label);
    if (addressId == null) {
      throw new IllegalStateException("Address not found for label " + label);
    }
    return addressId;
  }
}
