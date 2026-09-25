package com.mentorship.restaurant.customer;

import static org.assertj.core.api.Assertions.assertThat;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class DeleteCustomerEndpointTest {

  // Every user this class creates has an email under this prefix, so cleanup can never reach a
  // seeded user.
  private static final String TEST_EMAIL_PREFIX = "delete.customer.test.";
  private static final String CUSTOMER_EMAIL = TEST_EMAIL_PREFIX + "sara@example.com";
  private static final long NILE_KITCHEN = 1L;
  private static final long KOFTA = 1L;

  @Autowired private RestTestClient client;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  @AfterEach
  void deleteCreatedUsers() {
    // customers rows follow by ON DELETE CASCADE, and so do the carts, cart items and orders
    // this class seeds for them.
    jdbcTemplate.update("DELETE FROM users WHERE user_email LIKE ?", TEST_EMAIL_PREFIX + "%");
  }

  @Test
  void deletesTheCustomerSoTheyAreNoLongerFound() {
    Long customerId = createCustomer();

    deleteCustomer(customerId).expectStatus().isNoContent();

    client
        .get()
        .uri("/api/v1/customers/{customerId}", customerId)
        .exchange()
        .expectStatus()
        .isNotFound();
    deleteCustomer(customerId)
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Customer not found");
  }

  @Test
  void deletesTheCustomersCart() {
    Long customerId = createCustomer();
    client
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {"customer_id": %d, "menu_item_id": %d, "quantity": 1}
            """
                .formatted(customerId, KOFTA))
        .exchange()
        .expectStatus()
        .isCreated();

    deleteCustomer(customerId).expectStatus().isNoContent();

    Integer carts =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM carts WHERE customer_id = ?", Integer.class, customerId);
    assertThat(carts).isZero();
  }

  @Test
  void rejectsACustomerWithAnActiveOrder() {
    Long customerId = createCustomer();
    jdbcTemplate.update(
        """
        INSERT INTO orders
          (customer_id, restaurant_id, order_status, order_total, order_created_at)
        VALUES (?, ?, 'PLACED', 370.00, ?)
        """,
        customerId,
        NILE_KITCHEN,
        OffsetDateTime.now());

    deleteCustomer(customerId)
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Customer has active orders");

    client.get().uri("/api/v1/customers/{customerId}", customerId).exchange().expectStatus().isOk();
  }

  @Test
  void rejectsAnUnknownCustomer() {
    deleteCustomer(999999L)
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Customer not found");
  }

  @Test
  void hidesADeletedCustomersAddresses() {
    Long customerId = createCustomer();
    deleteCustomer(customerId).expectStatus().isNoContent();

    client
        .get()
        .uri("/api/v1/addresses?customerId={customerId}", customerId)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Customer not found");
  }

  private RestTestClient.ResponseSpec deleteCustomer(Long customerId) {
    return client.delete().uri("/api/v1/customers/{customerId}", customerId).exchange();
  }

  private Long createCustomer() {
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
}
