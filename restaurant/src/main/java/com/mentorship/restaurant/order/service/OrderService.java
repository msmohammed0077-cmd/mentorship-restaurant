package com.mentorship.restaurant.order.service;

import com.mentorship.restaurant.order.model.OrderCursor;
import com.mentorship.restaurant.order.model.entity.ActorRole;
import com.mentorship.restaurant.order.model.entity.OrderTransition;
import com.mentorship.restaurant.order.model.entity.RejectionReason;
import com.mentorship.restaurant.order.model.request.CreateOrderRequest;
import com.mentorship.restaurant.order.model.response.OrderHistoryResponse;
import com.mentorship.restaurant.order.model.response.OrderRatingResponse;
import com.mentorship.restaurant.order.model.response.OrderResponse;
import com.mentorship.restaurant.order.model.response.OrderStatusResponse;
import com.mentorship.restaurant.order.service.handler.AcceptOrderHandler;
import com.mentorship.restaurant.order.service.handler.AutoRejectStaleOrdersHandler;
import com.mentorship.restaurant.order.service.handler.CancelOrderHandler;
import com.mentorship.restaurant.order.service.handler.CreateOrderHandler;
import com.mentorship.restaurant.order.service.handler.RateOrderHandler;
import com.mentorship.restaurant.order.service.handler.RejectOrderHandler;
import com.mentorship.restaurant.order.service.handler.UpdateOrderStatusHandler;
import com.mentorship.restaurant.order.service.handler.ViewOrderHandler;
import com.mentorship.restaurant.order.service.handler.ViewOrderHistoryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {
  private final UpdateOrderStatusHandler updateOrderStatusHandler;
  private final AcceptOrderHandler acceptOrderHandler;
  private final RejectOrderHandler rejectOrderHandler;
  private final CancelOrderHandler cancelOrderHandler;
  private final AutoRejectStaleOrdersHandler autoRejectStaleOrdersHandler;
  private final ViewOrderHistoryHandler viewOrderHistoryHandler;
  private final RateOrderHandler rateOrderHandler;
  private final ViewOrderHandler viewOrderHandler;
  private final CreateOrderHandler createOrderHandler;

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

  public OrderStatusResponse cancel(Long orderId, Long customerId, ActorRole role) {
    return cancelOrderHandler.cancel(orderId, customerId, role);
  }

  public OrderHistoryResponse viewOrderHistory(
      Long customerId, ActorRole role, Integer limit, OrderCursor cursor) {
    return viewOrderHistoryHandler.viewOrderHistory(customerId, role, limit, cursor);
  }

  public OrderRatingResponse rate(Long orderId, Long customerId, Integer score, String comment) {
    return rateOrderHandler.rate(orderId, customerId, score, comment);
  }

  public OrderResponse createOrder(CreateOrderRequest request) {
    return createOrderHandler.createOrder(request);
  }

  public OrderResponse viewOrderDetails(Long orderId) {
    return viewOrderHandler.viewOrder(orderId);
  }

  public void autoRejectStaleOrders() {
    autoRejectStaleOrdersHandler.autoRejectStaleOrders();
  }
}
