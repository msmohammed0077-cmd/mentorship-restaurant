package com.mentorship.restaurant.support;

import java.util.Optional;
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
  protected static final long OTHER_CUSTOMER = 1L;

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

  protected String updatedAddressBody(String label) {
    return """
        {
          "label": "%s",
          "line": "45 Ramses Street",
          "city": "Giza",
          "area": "Mohandessin",
          "note": "Ring twice"
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

  protected long insertAddress(String label, boolean isDefault, String createdAt) {
    Long addressId =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO addresses (
              customer_id,
              address_label,
              address_line,
              address_city,
              address_area,
              address_note,
              address_is_default,
              address_created_at
            )
            VALUES (?, ?, '12 Tahrir Street', 'Cairo', 'Dokki', 'Blue gate', ?, CAST(? AS TIMESTAMP WITH TIME ZONE))
            RETURNING address_id
            """,
            Long.class,
            CUSTOMER_WITHOUT_ADDRESSES,
            label,
            isDefault,
            createdAt);
    if (addressId == null) {
      throw new IllegalStateException("Address not created for label " + label);
    }
    return addressId;
  }

  protected boolean addressExists(long addressId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM addresses WHERE address_id = ?", Integer.class, addressId);
    return count != null && count > 0;
  }

  protected Optional<Boolean> defaultFlagForAddress(long addressId) {
    return jdbcTemplate
        .queryForList(
            "SELECT address_is_default FROM addresses WHERE address_id = ?",
            Boolean.class,
            addressId)
        .stream()
        .findFirst();
  }
}
