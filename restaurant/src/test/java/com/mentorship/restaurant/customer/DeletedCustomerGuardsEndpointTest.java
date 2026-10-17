package com.mentorship.restaurant.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.CustomerEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Operations that look up an address, cart or order rather than the customer must still treat a
 * soft-deleted customer as not found (#78). The token stays valid after the soft delete, so these
 * are reachable.
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

  /** A Nile Kitchen item, seeded by V2. */
  private static final long MENU_ITEM = 1L;

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
        asCustomer(customerId)
            .put()
            .uri("/api/v1/addresses/{addressId}", addressId)
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
        asCustomer(customerId)
            .put()
            .uri("/api/v1/addresses/{addressId}/default", addressId)
            .exchange());
  }

  @Test
  void refusesToDeleteADeletedCustomersAddress() {
    long customerId = insertCustomer(email("sara"));
    long addressId = addressIdForCustomer(jdbcTemplate, customerId);
    softDeleteCustomer(customerId);

    expectCustomerNotFound(
        asCustomer(customerId).delete().uri("/api/v1/addresses/{addressId}", addressId).exchange());
  }

  @Test
  void refusesToCancelADeletedCustomersOrder() {
    long customerId = insertCustomer(email("sara"));
    long orderId = insertOrder(customerId, "PLACED");
    softDeleteCustomer(customerId);

    expectCustomerNotFound(
        asCustomer(customerId).post().uri("/api/v1/orders/{orderId}/cancel", orderId).exchange());
  }

  @Test
  void refusesToRateADeletedCustomersOrder() {
    long customerId = insertCustomer(email("sara"));
    long orderId = insertOrder(customerId, "DELIVERED");
    softDeleteCustomer(customerId);

    expectCustomerNotFound(
        asCustomer(customerId)
            .post()
            .uri("/api/v1/orders/{orderId}/rating", orderId)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"score\": 5}")
            .exchange());
  }

  @Test
  void refusesToViewADeletedCustomersOrder() {
    long customerId = insertCustomer(email("sara"));
    long orderId = insertOrder(customerId, "PLACED");
    softDeleteCustomer(customerId);

    expectCustomerNotFound(
        asCustomer(customerId).get().uri("/api/v1/orders/{orderId}", orderId).exchange());
  }

  @Test
  void refusesToViewADeletedCustomersCart() {
    long customerId = insertCustomer(email("sara"));
    long cartId = cartWithOneItem(customerId);
    softDeleteCustomer(customerId);

    expectCustomerNotFound(
        asCustomer(customerId).get().uri("/api/v1/cart/{cartId}", cartId).exchange());
  }

  @Test
  void refusesToModifyADeletedCustomersCart() {
    long customerId = insertCustomer(email("sara"));
    long cartId = cartWithOneItem(customerId);
    long cartItemId = onlyCartItemId(cartId);
    softDeleteCustomer(customerId);

    expectCustomerNotFound(
        asCustomer(customerId)
            .put()
            .uri("/api/v1/cart/{cartId}/items/{cartItemId}", cartId, cartItemId)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"quantity\": 2}")
            .exchange());
  }

  @Test
  void refusesToClearADeletedCustomersCart() {
    long customerId = insertCustomer(email("sara"));
    long cartId = cartWithOneItem(customerId);
    softDeleteCustomer(customerId);

    expectCustomerNotFound(
        asCustomer(customerId).delete().uri("/api/v1/cart/{cartId}", cartId).exchange());
  }

  @Test
  void refusesToRemoveItemsFromADeletedCustomersCart() {
    long customerId = insertCustomer(email("sara"));
    long cartId = cartWithOneItem(customerId);
    long cartItemId = onlyCartItemId(cartId);
    softDeleteCustomer(customerId);

    expectCustomerNotFound(
        asCustomer(customerId)
            .delete()
            .uri("/api/v1/cart/{cartId}/items?cartItemIds={id}", cartId, cartItemId)
            .exchange());
  }

  @Test
  void refusesToCheckOutADeletedCustomersCart() {
    long customerId = insertCustomer(email("sara"));
    long cartId = cartWithOneItem(customerId);
    softDeleteCustomer(customerId);

    expectCustomerNotFound(
        asCustomer(customerId).post().uri("/api/v1/cart/{cartId}/checkout", cartId).exchange());

    assertThat(cartExists(cartId)).isTrue();
  }

  private long cartWithOneItem(long customerId) {
    long cartId = insertCart(customerId);
    insertCartItem(cartId, MENU_ITEM, 1);
    return cartId;
  }

  private long onlyCartItemId(long cartId) {
    Long cartItemId =
        jdbcTemplate.queryForObject(
            "SELECT cart_item_id FROM cart_items WHERE cart_id = ?", Long.class, cartId);
    if (cartItemId == null) {
      throw new IllegalStateException("No item in cart " + cartId);
    }
    return cartItemId;
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
