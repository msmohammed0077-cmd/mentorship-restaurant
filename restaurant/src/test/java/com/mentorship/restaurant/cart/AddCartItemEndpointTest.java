package com.mentorship.restaurant.cart;

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
class AddCartItemEndpointTest {

  /** Seeded cart-less by V6, reserved for this test. Carts 1 and 2 are never touched. */
  private static final long CUSTOMER = 3L;

  private static final long KOFTA = 1L; // restaurant 1, 185.00
  private static final long FALAFEL = 2L; // restaurant 1, 65.00
  private static final long CLASSIC_BURGER = 4L; // restaurant 2, 120.00
  private static final long WINGS = 7L; // restaurant 3, which is closed

  @Autowired private RestTestClient client;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  @AfterEach
  void clearThisCustomersCart() {
    // Requests run in the server's own transactions, so nothing rolls back.
    // Scoped to this customer: a blanket delete would destroy V3's seeded carts,
    // and Flyway will not re-run V3 to restore them.
    jdbcTemplate.update(
        "DELETE FROM cart_items WHERE cart_id IN (SELECT cart_id FROM carts WHERE customer_id = ?)",
        CUSTOMER);
    jdbcTemplate.update("DELETE FROM carts WHERE customer_id = ?", CUSTOMER);
  }

  @Test
  void addsAnItemAndCreatesTheCart() {
    client
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body(KOFTA, 2))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.customerId")
        .isEqualTo(3)
        .jsonPath("$.items.length()")
        .isEqualTo(1)
        .jsonPath("$.items[0].itemName")
        .isEqualTo("Kofta Platter")
        .jsonPath("$.items[0].quantity")
        .isEqualTo(2)
        .jsonPath("$.total")
        .isEqualTo(370.00);
  }

  @Test
  void rejectsAnItemAlreadyInTheCart() {
    addItem(KOFTA, 2);

    client
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body(KOFTA, 1))
        .exchange()
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Item is already in cart");
  }

  @Test
  void keepsSeparateLinesForDifferentItemsOfTheSameRestaurant() {
    addItem(KOFTA, 1);

    client
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body(FALAFEL, 1))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.items.length()")
        .isEqualTo(2)
        .jsonPath("$.total")
        .isEqualTo(250.00);
  }

  @Test
  void rejectsAnItemFromADifferentRestaurant() {
    addItem(KOFTA, 1);

    client
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body(CLASSIC_BURGER, 1))
        .exchange()
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Item is of different restaurant");
  }

  @Test
  void rejectsAClosedRestaurant() {
    client
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body(WINGS, 1))
        .exchange()
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant is closed");
  }

  @Test
  void rejectsAQuantityBeyondStock() {
    // Kofta is seeded with stock 50; the request's @Max allows up to 1000.
    client
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body(KOFTA, 999))
        .exchange()
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Item is not in stock");
  }

  @Test
  void rejectsAnUnknownCustomer() {
    client
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"customerId\": 9999, \"menuItemId\": 1, \"quantity\": 1}")
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void rejectsAnUnknownItem() {
    client
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body(9999L, 1))
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void rejectsAQuantityOfZero() {
    client
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body(KOFTA, 0))
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  private String body(long menuItemId, int quantity) {
    return """
        {"customerId": %d, "menuItemId": %d, "quantity": %d}
        """
        .formatted(CUSTOMER, menuItemId, quantity);
  }

  private void addItem(long menuItemId, int quantity) {
    client
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body(menuItemId, quantity))
        .exchange()
        .expectStatus()
        .isCreated();
  }
}
