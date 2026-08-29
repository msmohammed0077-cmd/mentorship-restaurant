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
  protected void resetCartFixtures() {
    // Requests run in the server's own transactions, so endpoint tests clean up explicitly.
    jdbcTemplate.update(
        "DELETE FROM cart_items WHERE cart_id IN (SELECT cart_id FROM carts WHERE customer_id = ?)",
        CUSTOMER_WITHOUT_CART);
    jdbcTemplate.update("DELETE FROM carts WHERE customer_id = ?", CUSTOMER_WITHOUT_CART);
    jdbcTemplate.update("UPDATE menu_items SET menu_item_stock = 50 WHERE menu_item_id = ?", KOFTA);
    jdbcTemplate.update(
        "UPDATE menu_items SET menu_item_stock = 75 WHERE menu_item_id = ?", FALAFEL);
    jdbcTemplate.update(
        "UPDATE menu_items SET menu_item_stock = 40 WHERE menu_item_id = ?", CLASSIC_BURGER);
    jdbcTemplate.update("UPDATE menu_items SET menu_item_stock = 30 WHERE menu_item_id = ?", WINGS);
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

  protected int stockFor(long menuItemId) {
    Integer stock =
        jdbcTemplate.queryForObject(
            "SELECT menu_item_stock FROM menu_items WHERE menu_item_id = ?",
            Integer.class,
            menuItemId);
    if (stock == null) {
      throw new IllegalStateException("Stock not found for menu item " + menuItemId);
    }
    return stock;
  }

  protected int cartItemCountFor(long cartId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM cart_items WHERE cart_id = ?", Integer.class, cartId);
    if (count == null) {
      throw new IllegalStateException("Cart item count not found for cart " + cartId);
    }
    return count;
  }
}
