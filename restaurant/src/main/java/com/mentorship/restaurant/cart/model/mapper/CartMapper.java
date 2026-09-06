package com.mentorship.restaurant.cart.model.mapper;

import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.model.entity.Cart;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CartMapper {

  private final CartItemMapper cartItemMapper;

  public CartResponse toResponse(Cart cart) {
    return new CartResponse(
        cart.getId(),
        cart.getCustomer().getId(),
        cartItemMapper.toResponseList(cart.getItems()),
        cart.getItems().stream()
            .map(
                item ->
                    item.getItemPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
  }
}
