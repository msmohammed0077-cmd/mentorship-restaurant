package com.mentorship.restaurant.order.service.createorder;

import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.order.model.request.CreateOrderRequest;
import com.mentorship.restaurant.order.model.response.OrderResponse;
import com.mentorship.restaurant.restaurant.service.RestaurantService;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RestaurantValidatorHandler extends OrderHandler {

  private final Cart cart;
  private final RestaurantService restaurantService;

  @Override
  public OrderResponse handle(CreateOrderRequest request, OrderResponse response) {
    // A cart holds one restaurant's items, and ItemsValidatorHandler has refused an empty cart.
    Long restaurantId = cart.getItems().get(0).getMenuItem().getMenu().getRestaurant().getId();

    restaurantService.ensureOrderable(restaurantId);

    return handleNext(request, response);
  }
}
