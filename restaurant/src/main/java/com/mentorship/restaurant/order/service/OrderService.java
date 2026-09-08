package com.mentorship.restaurant.order.service;

import com.mentorship.restaurant.order.model.entity.ActorRole;
import com.mentorship.restaurant.order.model.entity.OrderTransition;
import com.mentorship.restaurant.order.model.response.OrderStatusResponse;
import com.mentorship.restaurant.order.service.handler.UpdateOrderStatusHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {
  private final UpdateOrderStatusHandler updateOrderStatusHandler;

  public OrderStatusResponse startPreparing(Long orderId, Long restaurantId, ActorRole role) {
    return updateOrderStatusHandler.transition(
        orderId, OrderTransition.START_PREPARING, restaurantId, role);
  }

  public OrderStatusResponse readyForPickup(Long orderId, Long restaurantId, ActorRole role) {
    return updateOrderStatusHandler.transition(orderId, OrderTransition.READY, restaurantId, role);
  }
}
