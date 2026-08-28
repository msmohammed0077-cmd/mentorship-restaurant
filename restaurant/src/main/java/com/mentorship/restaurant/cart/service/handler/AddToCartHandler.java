package com.mentorship.restaurant.cart.service.handler;

import com.mentorship.restaurant.cart.controller.response.CartResponse;
import com.mentorship.restaurant.cart.exception.CartItemAlreadyExistsException;
import com.mentorship.restaurant.cart.exception.CustomerNotFoundException;
import com.mentorship.restaurant.cart.exception.DifferentRestaurantException;
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
    Customer customer =
        customerRepository
            .findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
    MenuItem menuItem =
        menuItemRepository
            .findById(menuItemId)
            .orElseThrow(() -> new MenuItemNotFoundException("Item not found"));

    Cart cart = cartRepository.findByCustomer_Id(customerId).orElse(null);

    validateRequest(cart, menuItem, quantity);

    if (cart == null) {
      cart = createCartFor(customer);
    }
    cart.getItems().add(newLine(cart, menuItem, quantity, note));

    return cartMapper.toResponse(cart);
  }

  /**
   * The checks that need loaded state. Quantity is already bounded by the request's validation
   * annotations, so nothing here re-checks it.
   */
  private void validateRequest(Cart cart, MenuItem menuItem, Integer quantity) {
    Restaurant restaurant = menuItem.getMenu().getRestaurant();
    ensureRestaurantOpen(restaurant);
    if (cart != null) {
      ensureItemNotAlreadyInCart(cart, menuItem);
      ensureSameRestaurant(cart, restaurant);
    }
    ensureStockAvailable(menuItem, quantity);
  }

  private void ensureRestaurantOpen(Restaurant restaurant) {
    if (!restaurant.isOpen()) {
      throw new RestaurantClosedException("Restaurant is closed");
    }
  }

  /**
   * Adding an item the cart already holds is an error, not an increment. Changing the quantity of
   * an existing line is modify-cart's job.
   */
  private void ensureItemNotAlreadyInCart(Cart cart, MenuItem menuItem) {
    if (cartItemRepository
        .findByCart_IdAndMenuItem_Id(cart.getId(), menuItem.getId())
        .isPresent()) {
      throw new CartItemAlreadyExistsException("Item is already in cart");
    }
  }

  /** An empty cart has no restaurant of its own, so it accepts an item from any of them. */
  private void ensureSameRestaurant(Cart cart, Restaurant restaurant) {
    List<Long> restaurantIds = cartRepository.findRestaurantIdsByCartId(cart.getId());
    if (!restaurantIds.isEmpty() && !restaurantIds.contains(restaurant.getId())) {
      throw new DifferentRestaurantException("Item is of different restaurant");
    }
  }

  private void ensureStockAvailable(MenuItem menuItem, Integer quantity) {
    Integer available = menuItem.getStock();
    if (available == null || quantity > available) {
      throw new OutOfStockException("Item is not in stock");
    }
  }

  private Cart createCartFor(Customer customer) {
    Cart cart = new Cart();
    cart.setCustomer(customer);
    return cartRepository.save(cart);
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
