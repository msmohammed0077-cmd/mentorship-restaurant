package com.mentorship.restaurant.cart.service;

import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.service.handler.AddToCartHandler;
import com.mentorship.restaurant.cart.service.handler.ModifyCartItemHandler;
import org.springframework.stereotype.Service;

@Service
public class CartService {

  private final ModifyCartItemHandler modifyCartItemHandler;
  private final AddToCartHandler addToCartHandler;

  public CartService(
      ModifyCartItemHandler modifyCartItemHandler, AddToCartHandler addToCartHandler) {
    this.modifyCartItemHandler = modifyCartItemHandler;
    this.addToCartHandler = addToCartHandler;
  }

  public CartResponse addItem(Long customerId, Long menuItemId, Integer quantity, String note) {
    return addToCartHandler.addItem(customerId, menuItemId, quantity, note);
  }

  public CartResponse modifyItem(Long cartId, Long cartItemId, Integer quantity, String note) {
    return modifyCartItemHandler.modifyItem(cartId, cartItemId, quantity, note);
  }
}
