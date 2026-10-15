package com.mentorship.restaurant.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.CustomerEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.client.RestTestClient;

class DeleteCustomerEndpointTest extends CustomerEndpointTestSupport {

  private static final long KOFTA = 1L;

  @Override
  protected String emailPrefix() {
    return "delete.customer.test.";
  }

  @Test
  void deletesTheCustomerSoTheyAreNoLongerFound() {
    long customerId = insertCustomer(email("sara"));

    deleteCustomer(customerId).expectStatus().isNoContent();

    assertThat(isSoftDeleted(customerId)).isTrue();
    deleteCustomer(customerId)
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Customer not found");
  }

  /** The cart-by-id endpoints can still reach it: #104. */
  @Test
  void keepsTheCustomersCart() {
    long customerId = insertCustomer(email("sara"));
    long cartId = insertCart(customerId);
    insertCartItem(cartId, KOFTA, 1);

    deleteCustomer(customerId).expectStatus().isNoContent();

    assertThat(cartExists(cartId)).isTrue();
  }

  @Test
  void rejectsACustomerWithAnActiveOrder() {
    long customerId = insertCustomer(email("sara"));
    insertOrder(customerId, "PLACED");

    deleteCustomer(customerId)
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Customer has active orders");

    assertThat(isSoftDeleted(customerId)).isFalse();
  }

  @Test
  void hidesADeletedCustomersAddresses() {
    long customerId = insertCustomer(email("sara"));
    softDeleteCustomer(customerId);

    asCustomer(customerId)
        .get()
        .uri("/api/v1/addresses")
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Customer not found");
  }

  private RestTestClient.ResponseSpec deleteCustomer(long customerId) {
    return asCustomer(customerId).delete().uri("/api/v1/customers/me").exchange();
  }

  private boolean isSoftDeleted(long customerId) {
    Boolean deleted =
        jdbcTemplate.queryForObject(
            """
            SELECT u.user_deleted_at IS NOT NULL
            FROM users u JOIN customers c ON c.user_id = u.user_id
            WHERE c.customer_id = ?
            """,
            Boolean.class,
            customerId);
    return Boolean.TRUE.equals(deleted);
  }
}
