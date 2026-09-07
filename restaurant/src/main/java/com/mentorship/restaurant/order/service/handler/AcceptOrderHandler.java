package com.mentorship.restaurant.order.service.handler;

import com.mentorship.restaurant.order.model.entity.ActorRole;
import com.mentorship.restaurant.order.model.entity.OrderTransition;
import com.mentorship.restaurant.order.model.response.OrderStatusResponse;
import com.mentorship.restaurant.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Separate from RejectOrderHandler rather than a branch in one service: they take different
 * payloads, and only one of them compensates.
 */
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

    // Only after the transition succeeded — a 409 must record nothing.
    if (prepTimeMinutes != null) {
      orderRepository.updatePrepTime(orderId, prepTimeMinutes);
    }

    // Accept does not touch stock. Checkout decremented it and the order stands.
    return response;
  }
}
