package com.mentorship.restaurant.cart.service.handler;

import com.mentorship.restaurant.cart.exception.CartNotFoundException;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.mapper.CartMapper;
import com.mentorship.restaurant.cart.model.response.CartResponse;
import com.mentorship.restaurant.cart.repository.CartItemRepository;
import com.mentorship.restaurant.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClearCartHandler {

  private final CartItemRepository cartItemRepository;
  private final CartRepository cartRepository;
  private final CartMapper cartMapper;

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
