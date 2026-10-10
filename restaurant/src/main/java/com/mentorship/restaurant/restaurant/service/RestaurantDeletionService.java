package com.mentorship.restaurant.restaurant.service;

import com.mentorship.restaurant.order.service.OrderService;
import com.mentorship.restaurant.restaurant.exception.RestaurantHasActiveOrdersException;
import com.mentorship.restaurant.restaurant.model.entity.Restaurant;
import com.mentorship.restaurant.user.model.ActorRole;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Delete-restaurant, on its own because it is the only restaurant use-case that needs the order
 * domain, and order (through checkout) and cart need {@link RestaurantService}. Inside
 * RestaurantService it would close a constructor-injection cycle; here nothing points back at it.
 * Same shape as {@code CustomerDeletionService}.
 */
@Service
@RequiredArgsConstructor
public class RestaurantDeletionService {

  private final RestaurantService restaurantService;
  private final OrderService orderService;

  /** A soft delete. Past orders, ratings, menus and carts' lines are kept. */
  @Transactional
  public void deleteRestaurant(Long restaurantId, ActorRole role) {
    restaurantService.ensureAdmin(role, "delete a restaurant");
    Restaurant restaurant = restaurantService.findActiveRestaurant(restaurantId);

    ensureNoActiveOrders(restaurantId);

    // Managed entity: dirty checking issues the UPDATE, no save() needed.
    restaurant.getUser().setUserDeletedAt(OffsetDateTime.now());
  }

  /** A customer must never lose the restaurant of an order still in flight. */
  private void ensureNoActiveOrders(Long restaurantId) {
    if (orderService.hasActiveRestaurantOrders(restaurantId)) {
      throw new RestaurantHasActiveOrdersException("Restaurant has active orders");
    }
  }
}
