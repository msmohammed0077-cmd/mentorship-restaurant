package com.mentorship.restaurant.order.service;

import com.mentorship.restaurant.order.model.entity.ActorRole;
import com.mentorship.restaurant.order.model.entity.OrderTransition;
import com.mentorship.restaurant.order.model.entity.RejectionReason;
import com.mentorship.restaurant.order.model.response.OrderStatusResponse;
import com.mentorship.restaurant.order.service.handler.AcceptOrderHandler;
import com.mentorship.restaurant.order.service.handler.AutoRejectStaleOrdersHandler;
import com.mentorship.restaurant.order.service.handler.RejectOrderHandler;
import com.mentorship.restaurant.order.service.handler.UpdateOrderStatusHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

  private final UpdateOrderStatusHandler updateOrderStatusHandler;
  private final AcceptOrderHandler acceptOrderHandler;
  private final RejectOrderHandler rejectOrderHandler;
  private final AutoRejectStaleOrdersHandler autoRejectStaleOrdersHandler;

  public OrderStatusResponse startPreparing(Long orderId, Long restaurantId, ActorRole role) {
    return updateOrderStatusHandler.transition(
        orderId, OrderTransition.START_PREPARING, restaurantId, role);
  }

  public OrderStatusResponse readyForPickup(Long orderId, Long restaurantId, ActorRole role) {
    return updateOrderStatusHandler.transition(orderId, OrderTransition.READY, restaurantId, role);
  }

  public OrderStatusResponse accept(
      Long orderId, Long restaurantId, ActorRole role, Integer prepTimeMinutes) {
    return acceptOrderHandler.accept(orderId, restaurantId, role, prepTimeMinutes);
  }

  public OrderStatusResponse reject(
      Long orderId, Long restaurantId, ActorRole role, RejectionReason reason, String note) {
    return rejectOrderHandler.reject(orderId, restaurantId, role, reason, note);
  }

  public void autoRejectStaleOrders() {
    autoRejectStaleOrdersHandler.autoRejectStaleOrders();
  }
}
