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
import java.util.Optional;
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
      Long orderId, OrderTransition transition, Long actorId, ActorRole role) {
    ensureRoleOwnsTransition(transition, role);
    ensureActorOwnsOrder(orderId, actorId, role);
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

  private void ensureActorOwnsOrder(Long orderId, Long actorId, ActorRole role) {
    switch (role) {
      case SYSTEM -> ensureOrderExists(orderId);
      case RESTAURANT ->
          ensureOwnerMatches(
              orderRepository.findRestaurantIdById(orderId),
              actorId,
              "Order belongs to another restaurant");
      case CUSTOMER ->
          ensureOwnerMatches(
              orderRepository.findCustomerIdById(orderId),
              actorId,
              "Order belongs to another customer");
      case COURIER -> {
        ensureOrderExists(orderId);
        throw new OrderNotOwnedException("No courier is assigned to orders yet");
      }
    }
  }

  private void ensureOrderExists(Long orderId) {
    if (orderRepository.findRestaurantIdById(orderId).isEmpty()) {
      throw new OrderNotFoundException("Order not found");
    }
  }

  private void ensureOwnerMatches(Optional<Long> owner, Long actorId, String message) {
    Long ownerId = owner.orElseThrow(() -> new OrderNotFoundException("Order not found"));
    if (!ownerId.equals(actorId)) {
      throw new OrderNotOwnedException(message);
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

    history.setOrder(orderRepository.getReferenceById(orderId));
    history.setFromStatus(transition.from());
    history.setToStatus(transition.to());
    history.setActorRole(role);
    history.setAt(OffsetDateTime.now());
    orderStatusHistoryRepository.save(history);
  }
}
