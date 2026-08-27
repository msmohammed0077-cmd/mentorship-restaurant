package com.mentorship.restaurant.cart.service.handler;

import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.exception.CustomerNotFoundException;
import com.mentorship.restaurant.cart.exception.DifferentRestaurantException;
import com.mentorship.restaurant.cart.exception.InvalidQuantityException;
import com.mentorship.restaurant.cart.exception.MenuItemNotFoundException;
import com.mentorship.restaurant.cart.exception.OutOfStockException;
import com.mentorship.restaurant.cart.exception.RestaurantClosedException;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.entity.CartItem;
import com.mentorship.restaurant.cart.model.entity.Customer;
import com.mentorship.restaurant.cart.model.entity.MenuItem;
import com.mentorship.restaurant.cart.model.entity.Restaurant;
import com.mentorship.restaurant.cart.model.mapper.CartMapper;
import com.mentorship.restaurant.cart.repository.CartItemRepository;
import com.mentorship.restaurant.cart.repository.CartRepository;
import com.mentorship.restaurant.cart.repository.CustomerRepository;
import com.mentorship.restaurant.cart.repository.MenuItemRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddToCartHandler {

  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;
  private final CustomerRepository customerRepository;
  private final MenuItemRepository menuItemRepository;
  private final CartMapper cartMapper;

  public AddToCartHandler(
      CartRepository cartRepository,
      CartItemRepository cartItemRepository,
      CustomerRepository customerRepository,
      MenuItemRepository menuItemRepository,
      CartMapper cartMapper) {
    this.cartRepository = cartRepository;
    this.cartItemRepository = cartItemRepository;
    this.customerRepository = customerRepository;
    this.menuItemRepository = menuItemRepository;
    this.cartMapper = cartMapper;
  }

  @Transactional
  public CartResponse addItem(Long customerId, Long menuItemId, Integer quantity, String note) {
    validateQuantity(quantity);

    Customer customer =
        customerRepository
            .findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
    MenuItem menuItem =
        menuItemRepository
            .findById(menuItemId)
            .orElseThrow(() -> new MenuItemNotFoundException("Item not found"));

    ensureRestaurantOpen(menuItem.getMenu().getRestaurant());

    Cart cart = cartRepository.findByCustomer_Id(customerId).orElse(null);

    Optional<CartItem> existingLine =
        cart == null
            ? Optional.empty()
            : cartItemRepository.findByCart_IdAndMenuItem_Id(cart.getId(), menuItem.getId());

    ensureStockAvailable(existingLine, quantity, menuItem);

    if (cart == null) {
      cart = createCartFor(customer);
    } else {
      ensureSameRestaurant(cart, menuItem.getMenu().getRestaurant());
    }

    if (existingLine.isPresent()) {
      increment(existingLine.get(), quantity, note);
    } else {
      cart.getItems().add(newLine(cart, menuItem, quantity, note));
    }

    return cartMapper.toResponse(cart);
  }

  private void validateQuantity(Integer quantity) {
    if (quantity == null || quantity <= 0) {
      throw new InvalidQuantityException("Quantity must be greater than zero");
    }
  }

  private void ensureRestaurantOpen(Restaurant restaurant) {
    if (!restaurant.isOpen()) {
      throw new RestaurantClosedException("Restaurant is closed");
    }
  }

  /**
   * Checked against the resulting quantity, so repeated adds cannot walk past available stock one
   * call at a time.
   */
  private void ensureStockAvailable(
      Optional<CartItem> existingLine, Integer requested, MenuItem menuItem) {
    int alreadyInCart = existingLine.map(CartItem::getQuantity).orElse(0);
    Integer available = menuItem.getStock();
    if (available == null || alreadyInCart + requested > available) {
      throw new OutOfStockException("Item is not in stock");
    }
  }

  /** An empty cart has no restaurant of its own, so it accepts an item from any of them. */
  private void ensureSameRestaurant(Cart cart, Restaurant restaurant) {
    List<Long> restaurantIds = cartRepository.findRestaurantIdsByCartId(cart.getId());
    if (!restaurantIds.isEmpty() && !restaurantIds.contains(restaurant.getId())) {
      throw new DifferentRestaurantException("Item is of different restaurant");
    }
  }

  private Cart createCartFor(Customer customer) {
    Cart cart = new Cart();
    cart.setCustomer(customer);
    return cartRepository.save(cart);
  }

  private void increment(CartItem line, Integer quantity, String note) {
    line.setQuantity(line.getQuantity() + quantity);
    if (note != null) {
      line.setNote(note);
    }
  }

  private CartItem newLine(Cart cart, MenuItem menuItem, Integer quantity, String note) {
    CartItem line = new CartItem();
    line.setCart(cart);
    line.setMenuItem(menuItem);
    line.setQuantity(quantity);
    line.setNote(note);
    // Captured from the menu item, so a later menu price change does not reprice
    // what is already in the cart.
    line.setItemPrice(menuItem.getItemPrice());
    return cartItemRepository.save(line);
  }
}
