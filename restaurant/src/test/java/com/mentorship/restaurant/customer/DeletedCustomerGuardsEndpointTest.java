package com.mentorship.restaurant.customer;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Operations that look up an address or order rather than the customer must still treat a
 * soft-deleted customer as not found (#78).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class DeletedCustomerGuardsEndpointTest {

  // Every user this class creates has an email under this prefix, so cleanup can never reach a
  // seeded user.
  private static final String TEST_EMAIL_PREFIX = "deleted.guards.test.";
  private static final String CUSTOMER_EMAIL = TEST_EMAIL_PREFIX + "sara@example.com";
  private static final long NILE_KITCHEN = 1L;
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

  @Autowired private RestTestClient client;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  @AfterEach
  void deleteCreatedUsers() {
    // customers, addresses, orders and their ratings follow by ON DELETE CASCADE.
    jdbcTemplate.update("DELETE FROM users WHERE user_email LIKE ?", TEST_EMAIL_PREFIX + "%");
  }

  @Test
  void refusesToUpdateADeletedCustomersAddress() {
    long customerId = createCustomer();
    long addressId = addAddress(customerId);
    deleteCustomer(customerId);

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
    long customerId = createCustomer();
    long addressId = addAddress(customerId);
    deleteCustomer(customerId);

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
    long customerId = createCustomer();
    long addressId = addAddress(customerId);
    deleteCustomer(customerId);

    expectCustomerNotFound(
        client
            .delete()
            .uri("/api/v1/addresses/{addressId}?customerId={customerId}", addressId, customerId)
            .exchange());
  }

  @Test
  void refusesToCancelADeletedCustomersOrder() {
    long customerId = createCustomer();
    long orderId = seedOrder(customerId, "PLACED");
    // DELETE /customers refuses a customer with a PLACED order (409), so the API cannot produce
    // this state today. It can still arise — an order placed concurrently with the delete, or a
    // future rule change — so the marker is set directly to prove the guard holds regardless.
    jdbcTemplate.update(
        """
        UPDATE users SET user_deleted_at = now()
        WHERE user_id = (SELECT user_id FROM customers WHERE customer_id = ?)
        """,
        customerId);

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
    long customerId = createCustomer();
    long orderId = seedOrder(customerId, "DELIVERED");
    deleteCustomer(customerId);

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

  private long createCustomer() {
    client
        .post()
        .uri("/api/v1/customers")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {
              "name": "Sara Youssef",
              "email": "%s",
              "password": "s3cret-pass"
            }
            """
                .formatted(CUSTOMER_EMAIL))
        .exchange()
        .expectStatus()
        .isCreated();
    return jdbcTemplate.queryForObject(
        """
        SELECT c.customer_id FROM customers c JOIN users u ON u.user_id = c.user_id
        WHERE u.user_email = ?
        """,
        Long.class,
        CUSTOMER_EMAIL);
  }

  private long addAddress(long customerId) {
    client
        .post()
        .uri("/api/v1/addresses?customerId={customerId}", customerId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(ADDRESS_BODY)
        .exchange()
        .expectStatus()
        .isCreated();
    return jdbcTemplate.queryForObject(
        "SELECT address_id FROM addresses WHERE customer_id = ?", Long.class, customerId);
  }

  private long seedOrder(long customerId, String status) {
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO orders
          (customer_id, restaurant_id, order_status, order_total, order_created_at)
        VALUES (?, ?, ?, 370.00, ?)
        RETURNING order_id
        """,
        Long.class,
        customerId,
        NILE_KITCHEN,
        status,
        OffsetDateTime.now());
  }

  private void deleteCustomer(long customerId) {
    client
        .delete()
        .uri("/api/v1/customers/{customerId}", customerId)
        .exchange()
        .expectStatus()
        .isNoContent();
  }
}
