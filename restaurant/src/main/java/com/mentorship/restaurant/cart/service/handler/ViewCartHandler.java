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

@Service
public class ViewCartHandler {
  private final CartItemRepository cartItemRepository;
  private final CartRepository cartRepository;
  private final CartMapper cartMapper;

  public ViewCartHandler(
      CartItemRepository cartItemRepository, CartRepository cartRepository, CartMapper cartMapper) {
    this.cartItemRepository = cartItemRepository;
    this.cartRepository = cartRepository;
    this.cartMapper = cartMapper;
  }

  public CartResponse viewCart(Long cartId) {
    Cart cart =
        cartRepository
            .findById(cartId)
            .orElseThrow(() -> new CartItemNotFoundException("Cart not found"));
    List<CartItem> cartItemsList =
        cartItemRepository.findAllByCart_Id(cartId).orElse(Collections.emptyList());

    if (cartItemsList.isEmpty()) {
      return cartMapper.toResponse(cart);
    }

    cartItemsList.forEach(
        cartItem -> {
          if (cartItem.getMenuItem().getStock() == null
              || cartItem.getQuantity() > cartItem.getMenuItem().getStock()) {
            throw new CartItemNotFoundException(
                "Cart item with ID " + cartItem.getId() + " exceeds available stock");
          }
        });

    return cartMapper.toResponse(cart);
  }
}
