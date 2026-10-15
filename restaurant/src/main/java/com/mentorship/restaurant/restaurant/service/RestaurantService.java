package com.mentorship.restaurant.restaurant.service;

import com.mentorship.restaurant.restaurant.exception.MenuItemNotFoundException;
import com.mentorship.restaurant.restaurant.exception.OutOfStockException;
import com.mentorship.restaurant.restaurant.exception.RestaurantAccessDeniedException;
import com.mentorship.restaurant.restaurant.model.entity.MenuItem;
import com.mentorship.restaurant.restaurant.repository.MenuItemRepository;
import com.mentorship.restaurant.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Menu items and their stock, and restaurant ownership, for the cart and order domains. No
 * controller yet: the restaurant and menu CRUD (#85-#101) adds one. Every method joins the caller's
 * transaction.
 */
@Service
@RequiredArgsConstructor
public class RestaurantService {

  private final MenuItemRepository menuItemRepository;
  private final RestaurantRepository restaurantRepository;

  /**
   * The restaurant is the caller's, else a 403. One that does not exist is not theirs either. For
   * the order-status endpoints, which name the restaurant they act for.
   */
  public void ensureOwnedBy(Long restaurantId, Long userId) {
    if (!restaurantRepository.existsByIdAndUser_Id(restaurantId, userId)) {
      throw new RestaurantAccessDeniedException("Restaurant belongs to another owner");
    }
  }

  public MenuItem findMenuItem(Long menuItemId) {
    return menuItemRepository
        .findById(menuItemId)
        .orElseThrow(() -> new MenuItemNotFoundException("Item not found"));
  }

  /**
   * One conditional UPDATE, so two checkouts cannot both take the last unit. It clears the
   * persistence context: re-read anything loaded before it.
   */
  public void decrementStock(Long menuItemId, Integer quantity) {
    if (menuItemRepository.decrementStockIfAvailable(menuItemId, quantity) == 0) {
      throw new OutOfStockException("Requested quantity exceeds available stock");
    }
  }

  public void restoreStock(Long menuItemId, Integer quantity) {
    menuItemRepository.incrementStock(menuItemId, quantity);
  }
}
