package com.mentorship.restaurant.cart.service.handler;

import com.mentorship.restaurant.cart.controller.response.CheckoutCartResponse;
import com.mentorship.restaurant.cart.exception.CartItemNotFoundException;
import com.mentorship.restaurant.cart.exception.InvalidQuantityException;
import com.mentorship.restaurant.cart.exception.OutOfStockException;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.entity.CartItem;
import com.mentorship.restaurant.cart.model.entity.MenuItem;
import com.mentorship.restaurant.cart.repository.CartRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutCartHandler {

  private final CartRepository cartRepository;

  public CheckoutCartHandler(CartRepository cartRepository) {
    this.cartRepository = cartRepository;
  }

  @Transactional
  public CheckoutCartResponse checkout(Long cartId) {
    Cart cart =
        cartRepository
            .findById(cartId)
            .orElseThrow(() -> new CartItemNotFoundException("Cart not found"));
    List<CartItem> cartItems = cart.getItems();
    if (cartItems.isEmpty()) {
      throw new InvalidQuantityException("Cart is empty");
    }

    cartItems.forEach(this::ensureStockAvailable);
    cartItems.forEach(this::decreaseStock);
    cartItems.clear();

    return new CheckoutCartResponse("SUCCESS", "Payment successful");
  }

  private void ensureStockAvailable(CartItem cartItem) {
    MenuItem menuItem = cartItem.getMenuItem();
    Integer availableStock = menuItem.getStock();
    if (availableStock == null || cartItem.getQuantity() > availableStock) {
      throw new OutOfStockException("Requested quantity exceeds available stock");
    }
  }

  private void decreaseStock(CartItem cartItem) {
    MenuItem menuItem = cartItem.getMenuItem();
    menuItem.setStock(menuItem.getStock() - cartItem.getQuantity());
  }
}
