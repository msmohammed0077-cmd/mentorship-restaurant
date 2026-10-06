package com.mentorship.restaurant.customer;

import com.mentorship.restaurant.support.CustomerEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Operations that look up an address or order rather than the customer must still treat a
 * soft-deleted customer as not found (#78).
 */
class DeletedCustomerGuardsEndpointTest extends CustomerEndpointTestSupport {

  private static final String ADDRESS_BODY =
      """
      {
        "label": "Home",
        "line": "12 Tahrir Street",
        "city": "Cairo",
        "area": "Dokki",
        "note": "Blue gate"
      }
      """;

  @Override
  protected String emailPrefix() {
    return "deleted.guards.test.";
  }

  @Test
  void refusesToUpdateADeletedCustomersAddress() {
    long customerId = insertCustomer(email("sara"));
    long addressId = addressIdForCustomer(jdbcTemplate, customerId);
    softDeleteCustomer(customerId);

    expectCustomerNotFound(
        client
            .put()
            .uri("/api/v1/addresses/{addressId}?customerId={customerId}", addressId, customerId)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ADDRESS_BODY)
            .exchange());
  }

  @Test
  void refusesToSetADeletedCustomersDefaultAddress() {
    long customerId = insertCustomer(email("sara"));
    long addressId = addressIdForCustomer(jdbcTemplate, customerId);
    softDeleteCustomer(customerId);

    expectCustomerNotFound(
        client
            .put()
            .uri(
                "/api/v1/addresses/{addressId}/default?customerId={customerId}",
                addressId,
                customerId)
            .exchange());
  }

  @Test
  void refusesToDeleteADeletedCustomersAddress() {
    long customerId = insertCustomer(email("sara"));
    long addressId = addressIdForCustomer(jdbcTemplate, customerId);
    softDeleteCustomer(customerId);

    expectCustomerNotFound(
        client
            .delete()
            .uri("/api/v1/addresses/{addressId}?customerId={customerId}", addressId, customerId)
            .exchange());
  }

  @Test
  void refusesToCancelADeletedCustomersOrder() {
    long customerId = insertCustomer(email("sara"));
    long orderId = insertOrder(customerId, "PLACED");
    softDeleteCustomer(customerId);

    expectCustomerNotFound(
        client
            .post()
            .uri(
                "/api/v1/orders/{orderId}/cancel?customerId={customerId}&role=CUSTOMER",
                orderId,
                customerId)
            .exchange());
  }

  @Test
  void refusesToRateADeletedCustomersOrder() {
    long customerId = insertCustomer(email("sara"));
    long orderId = insertOrder(customerId, "DELIVERED");
    softDeleteCustomer(customerId);

    expectCustomerNotFound(
        client
            .post()
            .uri("/api/v1/orders/{orderId}/rating", orderId)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"customer_id\": %d, \"score\": 5}".formatted(customerId))
            .exchange());
  }

  private void expectCustomerNotFound(RestTestClient.ResponseSpec response) {
    response
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Customer not found");
  }
}
