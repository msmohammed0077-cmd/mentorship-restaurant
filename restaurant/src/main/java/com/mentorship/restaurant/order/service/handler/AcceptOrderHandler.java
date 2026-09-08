package com.mentorship.restaurant.order.service.handler;

import com.mentorship.restaurant.order.model.entity.ActorRole;
import com.mentorship.restaurant.order.model.entity.OrderTransition;
import com.mentorship.restaurant.order.model.response.OrderStatusResponse;
import com.mentorship.restaurant.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AcceptOrderHandler {
  private final UpdateOrderStatusHandler updateOrderStatusHandler;
  private final OrderRepository orderRepository;

  @Transactional
  public OrderStatusResponse accept(
      Long orderId, Long restaurantId, ActorRole role, Integer prepTimeMinutes) {
    OrderStatusResponse response =
        updateOrderStatusHandler.transition(orderId, OrderTransition.ACCEPT, restaurantId, role);

    if (prepTimeMinutes != null) {
      orderRepository.updatePrepTime(orderId, prepTimeMinutes);
    }

    return response;
  }
}
