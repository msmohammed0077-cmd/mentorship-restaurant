package com.mentorship.restaurant.restaurant.service;

import com.mentorship.restaurant.restaurant.exception.MenuItemNotFoundException;
import com.mentorship.restaurant.restaurant.exception.OutOfStockException;
import com.mentorship.restaurant.restaurant.exception.RestaurantNotFoundException;
import com.mentorship.restaurant.restaurant.model.entity.MenuItem;
import com.mentorship.restaurant.restaurant.model.entity.Restaurant;
import com.mentorship.restaurant.restaurant.model.mapper.RestaurantMapper;
import com.mentorship.restaurant.restaurant.model.response.RestaurantResponse;
import com.mentorship.restaurant.restaurant.repository.MenuItemRepository;
import com.mentorship.restaurant.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Restaurants, and their menu items and stock. The public use-cases back {@link
 * com.mentorship.restaurant.restaurant.controller.RestaurantController}; the methods without
 * {@code @Transactional} serve the cart and order domains and join the caller's transaction.
 */
@Service
@RequiredArgsConstructor
public class RestaurantService {

  private final RestaurantRepository restaurantRepository;
  private final MenuItemRepository menuItemRepository;
  private final RestaurantMapper restaurantMapper;

  @Transactional(readOnly = true)
  public RestaurantResponse getRestaurant(Long restaurantId) {
    return restaurantMapper.toResponse(findActiveRestaurant(restaurantId));
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

  private Restaurant findActiveRestaurant(Long restaurantId) {
    return restaurantRepository
        .findActiveById(restaurantId)
        .orElseThrow(() -> new RestaurantNotFoundException("Restaurant not found"));
  }
}
