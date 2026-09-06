package com.mentorship.restaurant.cart.service.handler;

import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.exception.CartNotFoundException;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.mapper.CartMapper;
import com.mentorship.restaurant.cart.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ViewCartHandler {

  private final CartRepository cartRepository;
  private final CartMapper cartMapper;

  public ViewCartHandler(CartRepository cartRepository, CartMapper cartMapper) {
    this.cartRepository = cartRepository;
    this.cartMapper = cartMapper;
  }

  /**
   * Reports what is in the cart and nothing more. Stock is checked when an item is added, modified
   * and at checkout; a cart whose item has since outrun its stock is still a cart the customer is
   * entitled to look at.
   */
  @Transactional(readOnly = true)
  public CartResponse viewCart(Long cartId) {
    Cart cart =
        cartRepository
            .findById(cartId)
            .orElseThrow(() -> new CartNotFoundException("Cart not found"));

    return cartMapper.toResponse(cart);
  }
}
