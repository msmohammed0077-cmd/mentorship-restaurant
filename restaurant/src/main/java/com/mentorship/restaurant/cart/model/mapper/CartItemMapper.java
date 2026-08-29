package com.mentorship.restaurant.cart.model.mapper;

import com.mentorship.restaurant.cart.controller.response.CartItemResponse;
import com.mentorship.restaurant.cart.model.entity.CartItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CartItemMapper {

  public CartItemResponse toResponse(CartItem cartItem) {
    return new CartItemResponse(
        cartItem.getId(),
        cartItem.getMenuItem().getId(),
        cartItem.getMenuItem().getName(),
        cartItem.getMenuItem().getMenuItemCode(),
        cartItem.getNote(),
        cartItem.getItemPrice(),
        cartItem.getQuantity());
  }

  public List<CartItemResponse> toResponseList(List<CartItem> cartItems) {
    return cartItems.stream().map(this::toResponse).toList();
  }
}
