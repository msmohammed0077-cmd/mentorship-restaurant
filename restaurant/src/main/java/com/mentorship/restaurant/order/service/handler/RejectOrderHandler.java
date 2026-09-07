package com.mentorship.restaurant.order.service.handler;

import com.mentorship.restaurant.order.exception.ReservedRejectionReasonException;
import com.mentorship.restaurant.order.model.entity.ActorRole;
import com.mentorship.restaurant.order.model.entity.OrderTransition;
import com.mentorship.restaurant.order.model.entity.RejectionReason;
import com.mentorship.restaurant.order.model.response.OrderStatusResponse;
import com.mentorship.restaurant.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RejectOrderHandler {

  private final UpdateOrderStatusHandler updateOrderStatusHandler;
  private final CompensateOrderHandler compensateOrderHandler;
  private final OrderRepository orderRepository;

  @Transactional
  public OrderStatusResponse reject(
      Long orderId, Long restaurantId, ActorRole role, RejectionReason reason, String note) {
    ensureReasonIsAvailableTo(role, reason);

    OrderStatusResponse response =
        updateOrderStatusHandler.transition(orderId, OrderTransition.REJECT, restaurantId, role);

    orderRepository.updateRejectionReason(orderId, reason, note);

    // Only after the transition succeeded. Restocking an order that was not
    // actually moved would hand back stock twice.
    compensateOrderHandler.compensate(orderId);

    return response;
  }

  private void ensureReasonIsAvailableTo(ActorRole role, RejectionReason reason) {
    if (reason.isSystemOnly() && role != ActorRole.SYSTEM) {
      throw new ReservedRejectionReasonException(reason + " is reserved for the system");
    }
  }
}
