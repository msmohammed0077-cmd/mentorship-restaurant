package com.mentorship.restaurant.cart.service.handler;

import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.exception.CartNotFoundException;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.mapper.CartMapper;
import com.mentorship.restaurant.cart.repository.CartItemRepository;
import com.mentorship.restaurant.cart.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClearCartHandler {

  private final CartItemRepository cartItemRepository;
  private final CartRepository cartRepository;
  private final CartMapper cartMapper;

  /**
   * Existence is checked with existsById rather than findById on purpose. A Cart loaded before the
   * bulk delete would be left holding a stale item collection, and mapping it would describe rows
   * that no longer exist. Not loading one makes that mistake impossible; the Cart is read after the
   * delete instead.
   *
   * <p>Clearing an already-empty cart is a no-op that still returns the cart, so the operation is
   * idempotent.
   */
  @Transactional
  public CartResponse clearCart(Long cartId) {
    ensureCartExists(cartId);

    cartItemRepository.deleteAllByCart_Id(cartId);

    Cart cart =
        cartRepository
            .findById(cartId)
            .orElseThrow(() -> new CartNotFoundException("Cart not found"));

    return cartMapper.toResponse(cart);
  }

  private void ensureCartExists(Long cartId) {
    if (!cartRepository.existsById(cartId)) {
      throw new CartNotFoundException("Cart not found");
    }
  }
}
