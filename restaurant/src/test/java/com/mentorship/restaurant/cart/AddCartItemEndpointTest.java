package com.mentorship.restaurant.cart;

import static com.mentorship.restaurant.support.RestaurantEndpointTestSupport.insertMenuItem;
import static com.mentorship.restaurant.support.RestaurantEndpointTestSupport.insertRestaurant;
import static com.mentorship.restaurant.support.RestaurantEndpointTestSupport.softDeleteRestaurant;

import com.mentorship.restaurant.support.CartEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AddCartItemEndpointTest extends CartEndpointTestSupport {

  @Test
  void addsAnItemAndCreatesTheCart() {
    asCustomer(CUSTOMER_WITHOUT_CART)
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(addItemBody(KOFTA, 2))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.customer_id")
        .isEqualTo(CUSTOMER_WITHOUT_CART)
        .jsonPath("$.items.length()")
        .isEqualTo(1)
        .jsonPath("$.items[0].item_name")
        .isEqualTo("Kofta Platter")
        .jsonPath("$.items[0].quantity")
        .isEqualTo(2)
        .jsonPath("$.total")
        .isEqualTo(370.00);
  }

  @Test
  void rejectsAnItemAlreadyInTheCart() {
    addItem(KOFTA, 2);

    asCustomer(CUSTOMER_WITHOUT_CART)
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(addItemBody(KOFTA, 1))
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

    asCustomer(CUSTOMER_WITHOUT_CART)
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(addItemBody(FALAFEL, 1))
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

    asCustomer(CUSTOMER_WITHOUT_CART)
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(addItemBody(CLASSIC_BURGER, 1))
        .exchange()
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Item is of different restaurant");
  }

  @Test
  void rejectsAClosedRestaurant() {
    asCustomer(CUSTOMER_WITHOUT_CART)
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(addItemBody(WINGS, 1))
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
    asCustomer(CUSTOMER_WITHOUT_CART)
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(addItemBody(KOFTA, 999))
        .exchange()
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Item is not in stock");
  }

  @Test
  void rejectsAnUnknownItem() {
    asCustomer(CUSTOMER_WITHOUT_CART)
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(addItemBody(9999L, 1))
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void rejectsAnItemOfADeletedRestaurant() {
    long restaurantId =
        insertRestaurant(jdbcTemplate, "Deleted Grill", EMAIL_PREFIX + "grill@example.com", true);
    long menuItemId = insertMenuItem(jdbcTemplate, restaurantId);
    softDeleteRestaurant(jdbcTemplate, restaurantId);

    asCustomer(CUSTOMER_WITHOUT_CART)
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(addItemBody(menuItemId, 1))
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Item not found");
  }

  @Test
  void rejectsAQuantityOfZero() {
    asCustomer(CUSTOMER_WITHOUT_CART)
        .post()
        .uri("/api/v1/cart/items")
        .contentType(MediaType.APPLICATION_JSON)
        .body(addItemBody(KOFTA, 0))
        .exchange()
        .expectStatus()
        .isBadRequest();
  }
}
