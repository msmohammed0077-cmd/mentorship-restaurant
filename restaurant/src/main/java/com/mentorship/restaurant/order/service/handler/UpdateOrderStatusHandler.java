package com.mentorship.restaurant.order.service.handler;

import com.mentorship.restaurant.order.exception.IllegalOrderTransitionException;
import com.mentorship.restaurant.order.exception.OrderNotFoundException;
import com.mentorship.restaurant.order.exception.OrderNotOwnedException;
import com.mentorship.restaurant.order.exception.TransitionNotAllowedForRoleException;
import com.mentorship.restaurant.order.model.entity.ActorRole;
import com.mentorship.restaurant.order.model.entity.OrderStatusHistory;
import com.mentorship.restaurant.order.model.entity.OrderTransition;
import com.mentorship.restaurant.order.model.mapper.OrderStatusMapper;
import com.mentorship.restaurant.order.model.response.OrderStatusResponse;
import com.mentorship.restaurant.order.repository.OrderRepository;
import com.mentorship.restaurant.order.repository.OrderStatusHistoryRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateOrderStatusHandler {

  private final OrderRepository orderRepository;
  private final OrderStatusHistoryRepository orderStatusHistoryRepository;
  private final OrderStatusMapper orderStatusMapper;

  @Transactional
  public OrderStatusResponse transition(
      Long orderId, OrderTransition transition, Long restaurantId, ActorRole role) {
    // Role before existence, so a caller with the wrong role learns nothing
    // about which order ids exist.
    ensureRoleOwnsTransition(transition, role);
    ensureRestaurantOwnsOrder(orderId, restaurantId);
    applyTransition(orderId, transition);
    recordHistory(orderId, transition, role);
    return orderStatusMapper.toResponse(orderId, transition.to());
  }

  private void ensureRoleOwnsTransition(OrderTransition transition, ActorRole role) {
    if (!transition.isPerformableBy(role)) {
      throw new TransitionNotAllowedForRoleException(
          "Role " + role + " may not perform this transition");
    }
  }

  private void ensureRestaurantOwnsOrder(Long orderId, Long restaurantId) {
    Long owner =
        orderRepository
            .findRestaurantIdById(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));
    if (!owner.equals(restaurantId)) {
      throw new OrderNotOwnedException("Order belongs to another restaurant");
    }
  }

  private void applyTransition(Long orderId, OrderTransition transition) {
    int updatedRows =
        orderRepository.updateStatusIfCurrent(orderId, transition.from(), transition.to());
    if (updatedRows == 0) {
      throw new IllegalOrderTransitionException("Order is not in status " + transition.from());
    }
  }

  private void recordHistory(Long orderId, OrderTransition transition, ActorRole role) {
    OrderStatusHistory history = new OrderStatusHistory();
    // The bulk update cleared the persistence context, so take a reference
    // rather than an entity loaded before it.
    history.setOrder(orderRepository.getReferenceById(orderId));
    history.setFromStatus(transition.from());
    history.setToStatus(transition.to());
    history.setActorRole(role);
    history.setAt(OffsetDateTime.now());
    orderStatusHistoryRepository.save(history);
  }
}
