package com.mentorship.restaurant.cart;

import static com.mentorship.restaurant.support.RestaurantEndpointTestSupport.insertMenuItem;
import static com.mentorship.restaurant.support.RestaurantEndpointTestSupport.insertRestaurant;
import static com.mentorship.restaurant.support.RestaurantEndpointTestSupport.softDeleteRestaurant;
import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.CartEndpointTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

class ModifyCartItemEndpointTest extends CartEndpointTestSupport {

  @Test
  void modifiesACartItem() {
    long cartId = createCartWithItem(KOFTA, 2);
    long cartItemId = cartItemIdFor(cartId);

    client
        .put()
        .uri("/api/v1/cart/{cartId}/items/{cartItemId}", cartId, cartItemId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            """
            {"quantity": 4, "note": "extra sauce"}
            """)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.id")
        .isEqualTo(cartId)
        .jsonPath("$.items.length()")
        .isEqualTo(1)
        .jsonPath("$.items[0].id")
        .isEqualTo(cartItemId)
        .jsonPath("$.items[0].quantity")
        .isEqualTo(4)
        .jsonPath("$.items[0].note")
        .isEqualTo("extra sauce")
        .jsonPath("$.total")
        .isEqualTo(740.00);
  }

  @Test
  void rejectsALineOfAClosedRestaurant() {
    long cartId = insertCartWithItem(WINGS, 1);
    long cartItemId = cartItemIdFor(cartId);

    modify(cartId, cartItemId, 4)
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Restaurant is closed");

    assertThat(quantityOf(cartItemId)).isEqualTo(1);
  }

  @Test
  void rejectsALineOfADeletedRestaurant() {
    long restaurantId =
        insertRestaurant(jdbcTemplate, "Deleted Grill", EMAIL_PREFIX + "grill@example.com", true);
    long menuItemId = insertMenuItem(jdbcTemplate, restaurantId);
    long cartId = insertCartWithItem(menuItemId, 1);
    long cartItemId = cartItemIdFor(cartId);
    softDeleteRestaurant(jdbcTemplate, restaurantId);

    modify(cartId, cartItemId, 4)
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Item not found");

    assertThat(quantityOf(cartItemId)).isEqualTo(1);
  }

  private RestTestClient.ResponseSpec modify(long cartId, long cartItemId, int quantity) {
    return client
        .put()
        .uri("/api/v1/cart/{cartId}/items/{cartItemId}", cartId, cartItemId)
        .contentType(MediaType.APPLICATION_JSON)
        .body("{\"quantity\": %d}".formatted(quantity))
        .exchange();
  }

  private int quantityOf(long cartItemId) {
    Integer quantity =
        jdbcTemplate.queryForObject(
            "SELECT cart_item_quantity FROM cart_items WHERE cart_item_id = ?",
            Integer.class,
            cartItemId);
    if (quantity == null) {
      throw new IllegalStateException("Cart item not found: " + cartItemId);
    }
    return quantity;
  }
}
