package com.mentorship.restaurant.cart.service.handler;

import com.mentorship.restaurant.cart.exception.CartNotFoundException;
import com.mentorship.restaurant.cart.exception.EmptyCartException;
import com.mentorship.restaurant.cart.exception.OutOfStockException;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.response.CheckoutCartResponse;
import com.mentorship.restaurant.cart.repository.CartItemRepository;
import com.mentorship.restaurant.cart.repository.CartRepository;
import com.mentorship.restaurant.cart.repository.MenuItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckoutCartHandler {

  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;
  private final MenuItemRepository menuItemRepository;

  private record Line(Long menuItemId, Integer quantity) {}

  @Transactional
  public CheckoutCartResponse checkout(Long cartId) {
    Cart cart =
        cartRepository
            .findById(cartId)
            .orElseThrow(() -> new CartNotFoundException("Cart not found"));

    List<Line> lines = linesOf(cart);
    if (lines.isEmpty()) {
      throw new EmptyCartException("Cart is empty");
    }

    lines.forEach(this::decrementStockAtomically);
    cartItemRepository.deleteAllByCart_Id(cartId);

    return new CheckoutCartResponse("SUCCESS", "Payment successful");
  }

  private List<Line> linesOf(Cart cart) {
    return cart.getItems().stream()
        .map(cartItem -> new Line(cartItem.getMenuItem().getId(), cartItem.getQuantity()))
        .toList();
  }

  private void decrementStockAtomically(Line line) {
    int updatedRows =
        menuItemRepository.decrementStockIfAvailable(line.menuItemId(), line.quantity());
    if (updatedRows == 0) {
      throw new OutOfStockException("Requested quantity exceeds available stock");
    }
  }
}
