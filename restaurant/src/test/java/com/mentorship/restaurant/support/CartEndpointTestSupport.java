package com.mentorship.restaurant.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public abstract class CartEndpointTestSupport {

  protected static final long CUSTOMER_WITHOUT_CART = 3L;
  protected static final long KOFTA = 1L;
  protected static final long FALAFEL = 2L;
  protected static final long CLASSIC_BURGER = 4L;
  protected static final long WINGS = 7L;

  @Autowired protected RestTestClient client;
  @Autowired protected JdbcTemplate jdbcTemplate;

  @BeforeEach
  @AfterEach
  protected void clearTestCustomersCart() {
    // Requests run in the server's own transactions, so endpoint tests clean up explicitly.
    jdbcTemplate.update(
        "DELETE FROM cart_items WHERE cart_id IN (SELECT cart_id FROM carts WHERE customer_id = ?)",
        CUSTOMER_WITHOUT_CART);
    jdbcTemplate.update("DELETE FROM carts WHERE customer_id = ?", CUSTOMER_WITHOUT_CART);
  }

  protected String addItemBody(long menuItemId, int quantity) {
    return """
        {"customerId": %d, "menuItemId": %d, "quantity": %d}
        """
        .formatted(CUSTOMER_WITHOUT_CART, menuItemId, quantity);
  }

  protected void addItem(long menuItemId, int quantity) {
    client
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(addItemBody(menuItemId, quantity))
        .exchange()
        .expectStatus()
        .isCreated();
  }

  protected long createCartWithItem(long menuItemId, int quantity) {
    addItem(menuItemId, quantity);
    return cartIdForCustomer(CUSTOMER_WITHOUT_CART);
  }

  protected long cartIdForCustomer(long customerId) {
    Long cartId =
        jdbcTemplate.queryForObject(
            "SELECT cart_id FROM carts WHERE customer_id = ?", Long.class, customerId);
    if (cartId == null) {
      throw new IllegalStateException("Cart not found for customer " + customerId);
    }
    return cartId;
  }

  protected long cartItemIdFor(long cartId) {
    Long cartItemId =
        jdbcTemplate.queryForObject(
            "SELECT cart_item_id FROM cart_items WHERE cart_id = ? ORDER BY cart_item_id LIMIT 1",
            Long.class,
            cartId);
    if (cartItemId == null) {
      throw new IllegalStateException("Cart item not found for cart " + cartId);
    }
    return cartItemId;
  }
}
