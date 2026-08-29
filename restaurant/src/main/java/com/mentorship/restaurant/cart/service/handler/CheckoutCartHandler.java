package com.mentorship.restaurant.cart.service.handler;

import com.mentorship.restaurant.cart.controller.response.CheckoutCartResponse;
import com.mentorship.restaurant.cart.exception.CartItemNotFoundException;
import com.mentorship.restaurant.cart.exception.InvalidQuantityException;
import com.mentorship.restaurant.cart.exception.OutOfStockException;
import com.mentorship.restaurant.cart.model.entity.CartItem;
import com.mentorship.restaurant.cart.repository.CartRepository;
import com.mentorship.restaurant.cart.repository.MenuItemRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutCartHandler {

  private final CartRepository cartRepository;
  private final MenuItemRepository menuItemRepository;

  public CheckoutCartHandler(CartRepository cartRepository, MenuItemRepository menuItemRepository) {
    this.cartRepository = cartRepository;
    this.menuItemRepository = menuItemRepository;
  }

  /** Performs checkout by atomically decrementing menu item stock and then clearing the cart. */
  @Transactional
  public CheckoutCartResponse checkout(Long cartId) {
    var cart =
        cartRepository
            .findById(cartId)
            .orElseThrow(() -> new CartItemNotFoundException("Cart not found"));
    List<CartItem> cartItems = cart.getItems();
    if (cartItems.isEmpty()) {
      throw new InvalidQuantityException("Cart is empty");
    }

    cartItems.forEach(this::decrementStockAtomically);
    cartItems.clear();

    return new CheckoutCartResponse("SUCCESS", "Payment successful");
  }

  private void decrementStockAtomically(CartItem cartItem) {
    int updatedRows =
        menuItemRepository.decrementStockIfAvailable(
            cartItem.getMenuItem().getId(), cartItem.getQuantity());
    if (updatedRows == 0) {
      throw new OutOfStockException("Requested quantity exceeds available stock");
    }
  }
}
