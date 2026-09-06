package com.mentorship.restaurant.cart;

import static org.assertj.core.api.Assertions.assertThat;

import com.mentorship.restaurant.support.CartEndpointTestSupport;
import org.junit.jupiter.api.Test;

class RemoveCartItemEndpointTest extends CartEndpointTestSupport {

  private static final long SEEDED_CART = 1L;

  @Test
  void removesOneOfTwoItems() {
    long cartId = createCartWithItem(KOFTA, 1);
    addItem(FALAFEL, 1);
    long falafelItemId = cartItemIdFor(cartId, FALAFEL);

    client
        .delete()
        .uri("/api/v1/cart/{cartId}/items?cartItemIds={id}", cartId, falafelItemId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.items.length()")
        .isEqualTo(1)
        .jsonPath("$.total")
        .isEqualTo(185.00);

    assertThat(cartItemCountFor(cartId)).isEqualTo(1);
  }

  /** The cart outlives its last item; clearCart already behaves this way. */
  @Test
  void removesTheLastItemAndKeepsTheCart() {
    long cartId = createCartWithItem(KOFTA, 2);
    long cartItemId = cartItemIdFor(cartId);

    client
        .delete()
        .uri("/api/v1/cart/{cartId}/items?cartItemIds={id}", cartId, cartItemId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.id")
        .isEqualTo(cartId)
        .jsonPath("$.customerId")
        .isEqualTo(CUSTOMER_WITHOUT_CART)
        .jsonPath("$.items.length()")
        .isEqualTo(0)
        .jsonPath("$.total")
        .isEqualTo(0);

    assertThat(cartExists(cartId)).isTrue();
  }

  /** The count assertion is the point: a partial delete must not survive the rejection. */
  @Test
  void rejectsAnUnknownCartItemAndRemovesNothing() {
    long cartId = createCartWithItem(KOFTA, 1);
    long cartItemId = cartItemIdFor(cartId);

    client
        .delete()
        .uri(
            "/api/v1/cart/{cartId}/items?cartItemIds={known}&cartItemIds={unknown}",
            cartId,
            cartItemId,
            999999L)
        .exchange()
        .expectStatus()
        .isNotFound();

    assertThat(cartItemCountFor(cartId)).isEqualTo(1);
  }

  /** Proves the delete is scoped by cart and not by item id alone. */
  @Test
  void rejectsACartItemBelongingToAnotherCart() {
    long cartId = createCartWithItem(KOFTA, 1);
    long otherCartsItemId = cartItemIdFor(SEEDED_CART);
    int otherCartItemCount = cartItemCountFor(SEEDED_CART);

    client
        .delete()
        .uri("/api/v1/cart/{cartId}/items?cartItemIds={id}", cartId, otherCartsItemId)
        .exchange()
        .expectStatus()
        .isNotFound();

    assertThat(cartItemCountFor(cartId)).isEqualTo(1);
    assertThat(cartItemCountFor(SEEDED_CART)).isEqualTo(otherCartItemCount);
  }

  /** A missing cart is reported as such, which only holds while the guard precedes the delete. */
  @Test
  void rejectsAnUnknownCart() {
    client
        .delete()
        .uri("/api/v1/cart/{cartId}/items?cartItemIds={id}", 999999L, 1L)
        .exchange()
        .expectStatus()
        .isNotFound()
        .expectBody()
        .jsonPath("$.message")
        .isEqualTo("Cart not found");
  }

  /** The same id twice deletes one row, which must not read as a missing item. */
  @Test
  void acceptsDuplicateIds() {
    long cartId = createCartWithItem(KOFTA, 1);
    long cartItemId = cartItemIdFor(cartId);

    client
        .delete()
        .uri(
            "/api/v1/cart/{cartId}/items?cartItemIds={id}&cartItemIds={id}",
            cartId,
            cartItemId,
            cartItemId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.items.length()")
        .isEqualTo(0);

    assertThat(cartItemCountFor(cartId)).isZero();
  }
}
