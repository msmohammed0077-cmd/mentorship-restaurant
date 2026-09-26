package com.mentorship.restaurant.cart.service.handler;

import com.mentorship.restaurant.cart.exception.CartItemNotFoundException;
import com.mentorship.restaurant.cart.exception.OutOfStockException;
import com.mentorship.restaurant.cart.model.entity.CartItem;
import com.mentorship.restaurant.cart.model.mapper.CartMapper;
import com.mentorship.restaurant.cart.model.response.CartResponse;
import com.mentorship.restaurant.cart.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModifyCartItemHandler {

  private final CartItemRepository cartItemRepository;
  private final CartMapper cartMapper;

  @Transactional
  public CartResponse modifyItem(Long cartId, Long cartItemId, Integer quantity, String note) {
    CartItem cartItem =
        cartItemRepository
            .findByIdAndCart_Id(cartItemId, cartId)
            .orElseThrow(() -> new CartItemNotFoundException("Cart item not found"));

    ensureStockAvailable(quantity, cartItem.getMenuItem().getStock());

    cartItem.setQuantity(quantity);
    cartItem.setNote(note);
    cartItem.setItemPrice(cartItem.getMenuItem().getItemPrice());

    return cartMapper.toResponse(cartItem.getCart());
  }

  private void ensureStockAvailable(Integer requestedQuantity, Integer availableStock) {
    if (availableStock == null || requestedQuantity > availableStock) {
      throw new OutOfStockException("Requested quantity exceeds available stock");
    }
  }
}
