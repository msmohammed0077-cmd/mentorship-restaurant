package com.mentorship.restaurant.cart.service;

import com.mentorship.restaurant.cart.model.response.CartResponse;
import com.mentorship.restaurant.cart.model.response.CheckoutCartResponse;
import com.mentorship.restaurant.cart.service.handler.AddToCartHandler;
import com.mentorship.restaurant.cart.service.handler.CheckoutCartHandler;
import com.mentorship.restaurant.cart.service.handler.ClearCartHandler;
import com.mentorship.restaurant.cart.service.handler.ModifyCartItemHandler;
import com.mentorship.restaurant.cart.service.handler.RemoveCartItemHandler;
import com.mentorship.restaurant.cart.service.handler.ViewCartHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartService {

  private final ModifyCartItemHandler modifyCartItemHandler;
  private final ViewCartHandler viewCartHandler;
  private final ClearCartHandler clearCartHandler;
  private final AddToCartHandler addToCartHandler;
  private final RemoveCartItemHandler removeCartItemHandler;
  private final CheckoutCartHandler checkoutCartHandler;

  public CartResponse addItem(Long customerId, Long menuItemId, Integer quantity, String note) {
    return addToCartHandler.addItem(customerId, menuItemId, quantity, note);
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

  public CartResponse removeCartItems(Long cartId, List<Long> cartItemIds) {
    return removeCartItemHandler.removeCartItems(cartId, cartItemIds);
  }

  public CheckoutCartResponse checkout(Long cartId) {
    return checkoutCartHandler.checkout(cartId);
  }
}
