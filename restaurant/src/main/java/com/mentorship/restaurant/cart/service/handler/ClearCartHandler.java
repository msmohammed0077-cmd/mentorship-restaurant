package com.mentorship.restaurant.cart.service.handler;

import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.exception.CartItemNotFoundException;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.entity.CartItem;
import com.mentorship.restaurant.cart.model.mapper.CartMapper;
import com.mentorship.restaurant.cart.repository.CartItemRepository;
import com.mentorship.restaurant.cart.repository.CartRepository;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClearCartHandler {
  private final CartItemRepository cartItemRepository;
  private final CartRepository cartRepository;
  private final CartMapper cartMapper;

  public ClearCartHandler(
      CartItemRepository cartItemRepository, CartRepository cartRepository, CartMapper cartMapper) {
    this.cartItemRepository = cartItemRepository;
    this.cartRepository = cartRepository;
    this.cartMapper = cartMapper;
  }

  @Transactional
  public CartResponse clearCart(Long cartId) {
    Cart cart =
        cartRepository
            .findById(cartId)
            .orElseThrow(() -> new CartItemNotFoundException("Cart not found"));
    List<CartItem> cartItems =
        cartItemRepository.findAllByCart_Id(cartId).orElse(Collections.emptyList());

    if (cartItems.isEmpty()) {
      return cartMapper.toResponse(cart);
    }

    // Clear items from the managed Cart entity so JPA orphanRemoval will delete them
    cart.getItems().clear();
    // Persist change to ensure DB is updated within the transaction
    cartRepository.save(cart);
    return cartMapper.toResponse(cart);
  }
}
