package com.mentorship.restaurant.order.service.handler;

import com.mentorship.restaurant.order.model.entity.ActorRole;
import com.mentorship.restaurant.order.model.entity.OrderTransition;
import com.mentorship.restaurant.order.model.response.OrderStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CancelOrderHandler {
  private final UpdateOrderStatusHandler updateOrderStatusHandler;
  private final CompensateOrderHandler compensateOrderHandler;

  @Transactional
  public OrderStatusResponse cancel(Long orderId, Long customerId, ActorRole role) {
    OrderStatusResponse response =
        updateOrderStatusHandler.transition(orderId, OrderTransition.CANCEL, customerId, role);

    compensateOrderHandler.compensate(orderId);

    return response;
  }
}
