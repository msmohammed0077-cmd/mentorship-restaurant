package com.mentorship.restaurant.cart.service;

import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.service.handler.ClearCartHandler;
import com.mentorship.restaurant.cart.service.handler.ModifyCartItemHandler;
import com.mentorship.restaurant.cart.service.handler.ViewCartHandler;
import org.springframework.stereotype.Service;

@Service
public class CartService {

  private final ModifyCartItemHandler modifyCartItemHandler;
  private final ViewCartHandler viewCartHandler;
  private final ClearCartHandler clearCartHandler;

  public CartService(ModifyCartItemHandler modifyCartItemHandler, ViewCartHandler viewCartHandler, ClearCartHandler clearCartHandler) {
    this.modifyCartItemHandler = modifyCartItemHandler;
    this.viewCartHandler = viewCartHandler;
      this.clearCartHandler = clearCartHandler;
  }

  public CartResponse modifyItem(Long cartId, Long cartItemId, Integer quantity, String note) {
    return modifyCartItemHandler.modifyItem(cartId, cartItemId, quantity, note);
  }

  public CartResponse viewCart(Long cartId) {
    return viewCartHandler.viewCart(cartId);
  }

  public CartResponse clearCart(Long cartId) {
    return clearCartHandler.clearCart(cartId);
  }
}
