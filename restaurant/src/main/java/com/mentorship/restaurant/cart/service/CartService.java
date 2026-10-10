package com.mentorship.restaurant.cart.service;

import com.mentorship.restaurant.cart.exception.CartItemAlreadyExistsException;
import com.mentorship.restaurant.cart.exception.CartItemNotFoundException;
import com.mentorship.restaurant.cart.exception.CartNotFoundException;
import com.mentorship.restaurant.cart.exception.DifferentRestaurantException;
import com.mentorship.restaurant.cart.exception.EmptyCartException;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.model.entity.CartItem;
import com.mentorship.restaurant.cart.model.mapper.CartMapper;
import com.mentorship.restaurant.cart.model.response.CartResponse;
import com.mentorship.restaurant.cart.model.response.CheckoutCartResponse;
import com.mentorship.restaurant.cart.repository.CartItemRepository;
import com.mentorship.restaurant.cart.repository.CartRepository;
import com.mentorship.restaurant.customer.model.entity.Customer;
import com.mentorship.restaurant.customer.service.CustomerService;
import com.mentorship.restaurant.restaurant.exception.OutOfStockException;
import com.mentorship.restaurant.restaurant.exception.RestaurantClosedException;
import com.mentorship.restaurant.restaurant.model.entity.MenuItem;
import com.mentorship.restaurant.restaurant.model.entity.Restaurant;
import com.mentorship.restaurant.restaurant.service.RestaurantService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;
  private final CustomerService customerService;
  private final RestaurantService restaurantService;
  private final CartMapper cartMapper;

  private record Line(Long menuItemId, Integer quantity) {}

  @Transactional
  public CartResponse addItem(Long customerId, Long menuItemId, Integer quantity, String note) {
    Customer customer = customerService.findActiveCustomer(customerId);
    MenuItem menuItem = restaurantService.findMenuItem(menuItemId);
    Cart cart = cartRepository.findByCustomer_Id(customerId).orElse(null);

    validateNewLine(cart, menuItem, quantity);

    if (cart == null) {
      cart = createCartFor(customer);
    }
    cart.getItems().add(newLine(cart, menuItem, quantity, note));
    return cartMapper.toResponse(cart);
  }

  @Transactional
  public CartResponse modifyItem(Long cartId, Long cartItemId, Integer quantity, String note) {
    CartItem cartItem =
        cartItemRepository
            .findByIdAndCart_Id(cartItemId, cartId)
            .orElseThrow(() -> new CartItemNotFoundException("Cart item not found"));

    ensureStockCovers(quantity, cartItem.getMenuItem().getStock());

    cartItem.setQuantity(quantity);
    cartItem.setNote(note);
    cartItem.setItemPrice(cartItem.getMenuItem().getItemPrice());

    return cartMapper.toResponse(cartItem.getCart());
  }

  @Transactional(readOnly = true)
  public CartResponse viewCart(Long cartId) {
    return cartMapper.toResponse(findCart(cartId));
  }

  @Transactional
  public CartResponse clearCart(Long cartId) {
    ensureCartExists(cartId);
    cartItemRepository.deleteAllByCart_Id(cartId);
    // Re-read after the bulk delete: anything loaded before it would be stale.
    return cartMapper.toResponse(findCart(cartId));
  }

  @Transactional
  public CartResponse removeCartItems(Long cartId, List<Long> cartItemIds) {
    ensureCartExists(cartId);
    Set<Long> requestedIds = new LinkedHashSet<>(cartItemIds);
    ensureAllItemsInCart(cartId, requestedIds);

    cartItemRepository.deleteAllByCart_IdAndIdIn(cartId, requestedIds);

    // Re-read after the bulk delete: anything loaded before it would be stale.
    return cartMapper.toResponse(findCart(cartId));
  }

  @Transactional
  public CheckoutCartResponse checkout(Long cartId) {
    // Take the lines out before the bulk delete below, which leaves a loaded cart stale.
    Cart cart = findCart(cartId);
    List<Line> lines = linesOf(cart);
    if (lines.isEmpty()) {
      throw new EmptyCartException("Cart is empty");
    }
    // A cart holds one restaurant's items: refuse a closed or deleted one, as create-order does.
    restaurantService.ensureOrderable(
        cart.getItems().get(0).getMenuItem().getMenu().getRestaurant().getId());

    lines.forEach(line -> restaurantService.decrementStock(line.menuItemId(), line.quantity()));
    cartItemRepository.deleteAllByCart_Id(cartId);

    return new CheckoutCartResponse("SUCCESS", "Payment successful");
  }

  /** The cart, else a 404. Create-order calls it too; joins the caller's transaction. */
  public Cart findCart(Long cartId) {
    return cartRepository
        .findById(cartId)
        .orElseThrow(() -> new CartNotFoundException("Cart not found"));
  }

  /** Create-order's last step once the order is saved; joins the caller's transaction. */
  public void deleteCart(Long cartId) {
    cartRepository.deleteById(cartId);
  }

  private void ensureCartExists(Long cartId) {
    if (!cartRepository.existsById(cartId)) {
      throw new CartNotFoundException("Cart not found");
    }
  }

  /**
   * The checks that need loaded state. Quantity is already bounded by the request's validation
   * annotations, so nothing here re-checks it.
   */
  private void validateNewLine(Cart cart, MenuItem menuItem, Integer quantity) {
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

  /**
   * An empty cart holds no line from another restaurant, so it accepts an item from any of them.
   */
  private void ensureSameRestaurant(Cart cart, Restaurant restaurant) {
    if (cartItemRepository.existsByCart_IdAndMenuItem_Menu_Restaurant_IdNot(
        cart.getId(), restaurant.getId())) {
      throw new DifferentRestaurantException("Item is of different restaurant");
    }
  }

  private void ensureStockAvailable(MenuItem menuItem, Integer quantity) {
    Integer available = menuItem.getStock();
    if (available == null || quantity > available) {
      throw new OutOfStockException("Item is not in stock");
    }
  }

  private void ensureStockCovers(Integer requestedQuantity, Integer availableStock) {
    if (availableStock == null || requestedQuantity > availableStock) {
      throw new OutOfStockException("Requested quantity exceeds available stock");
    }
  }

  private void ensureAllItemsInCart(Long cartId, Set<Long> requestedIds) {
    List<Long> present = cartItemRepository.findIdsByCart_IdAndIdIn(cartId, requestedIds);
    if (present.size() == requestedIds.size()) {
      return;
    }
    List<Long> missing = requestedIds.stream().filter(id -> !present.contains(id)).toList();
    throw new CartItemNotFoundException("Cart items not found: " + missing);
  }

  private Cart createCartFor(Customer customer) {
    return cartRepository.save(Cart.builder().customer(customer).build());
  }

  private CartItem newLine(Cart cart, MenuItem menuItem, Integer quantity, String note) {
    CartItem line =
        CartItem.builder()
            .cart(cart)
            .menuItem(menuItem)
            .quantity(quantity)
            .note(note)
            // Captured from the menu item, so a later menu price change does not reprice
            // what is already in the cart.
            .itemPrice(menuItem.getItemPrice())
            .build();
    return cartItemRepository.save(line);
  }

  private List<Line> linesOf(Cart cart) {
    return cart.getItems().stream()
        .map(cartItem -> new Line(cartItem.getMenuItem().getId(), cartItem.getQuantity()))
        .toList();
  }
}
