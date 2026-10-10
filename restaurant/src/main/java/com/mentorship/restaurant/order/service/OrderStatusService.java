package com.mentorship.restaurant.order.service;

import com.mentorship.restaurant.order.exception.IllegalOrderTransitionException;
import com.mentorship.restaurant.order.exception.OrderNotFoundException;
import com.mentorship.restaurant.order.exception.OrderNotOwnedException;
import com.mentorship.restaurant.order.exception.ReservedRejectionReasonException;
import com.mentorship.restaurant.order.exception.TransitionNotAllowedForRoleException;
import com.mentorship.restaurant.order.model.entity.OrderStatusHistory;
import com.mentorship.restaurant.order.model.entity.OrderTransition;
import com.mentorship.restaurant.order.model.entity.RejectionReason;
import com.mentorship.restaurant.order.model.mapper.OrderStatusMapper;
import com.mentorship.restaurant.order.model.response.OrderStatusResponse;
import com.mentorship.restaurant.order.repository.OrderItemRepository;
import com.mentorship.restaurant.order.repository.OrderLineProjection;
import com.mentorship.restaurant.order.repository.OrderOwnerProjection;
import com.mentorship.restaurant.order.repository.OrderRepository;
import com.mentorship.restaurant.order.repository.OrderStatusHistoryRepository;
import com.mentorship.restaurant.restaurant.service.RestaurantService;
import com.mentorship.restaurant.user.model.ActorRole;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class OrderStatusService {

  private static final Logger log = LoggerFactory.getLogger(OrderStatusService.class);
  private static final int AUTO_REJECT_AFTER_MINUTES = 15;

  private final OrderRepository orderRepository;
  private final OrderStatusHistoryRepository orderStatusHistoryRepository;
  private final OrderItemRepository orderItemRepository;
  private final RestaurantService restaurantService;
  private final OrderStatusMapper orderStatusMapper;
  private final OrderService orderService;
  private final TransactionTemplate transactionTemplate;

  @Transactional
  public OrderStatusResponse startPreparing(Long orderId, Long restaurantId, ActorRole role) {
    return transition(orderId, OrderTransition.START_PREPARING, restaurantId, role);
  }

  @Transactional
  public OrderStatusResponse readyForPickup(Long orderId, Long restaurantId, ActorRole role) {
    return transition(orderId, OrderTransition.READY, restaurantId, role);
  }

  @Transactional
  public OrderStatusResponse accept(
      Long orderId, Long restaurantId, ActorRole role, Integer prepTimeMinutes) {
    OrderStatusResponse response = transition(orderId, OrderTransition.ACCEPT, restaurantId, role);

    if (prepTimeMinutes != null) {
      orderRepository.updatePrepTime(orderId, prepTimeMinutes);
    }

    return response;
  }

  @Transactional
  public OrderStatusResponse reject(
      Long orderId, Long restaurantId, ActorRole role, RejectionReason reason, String note) {
    ensureReasonIsAvailableTo(role, reason);

    OrderStatusResponse response = transition(orderId, OrderTransition.REJECT, restaurantId, role);
    orderRepository.updateRejectionReason(orderId, reason, note);
    restoreStock(orderId);

    return response;
  }

  @Transactional
  public OrderStatusResponse cancel(Long orderId, Long customerId, ActorRole role) {
    OrderStatusResponse response = transition(orderId, OrderTransition.CANCEL, customerId, role);

    restoreStock(orderId);

    return response;
  }

  /**
   * Not transactional itself: each stale order is rejected in its own transaction, so one failure
   * neither rolls back nor stops the others. A plain {@code reject(...)} call from here would skip
   * the Spring proxy and run with no transaction at all.
   */
  public void autoRejectStaleOrders() {
    OffsetDateTime deadline = OffsetDateTime.now().minusMinutes(AUTO_REJECT_AFTER_MINUTES);
    List<Long> staleOrderIds = orderRepository.findStalePlacedOrderIds(deadline);

    for (Long orderId : staleOrderIds) {
      try {
        transactionTemplate.executeWithoutResult(
            status -> reject(orderId, null, ActorRole.SYSTEM, RejectionReason.NO_RESPONSE, null));
      } catch (RuntimeException exception) {
        log.warn("Auto-reject skipped order {}", orderId, exception);
      }
    }
  }

  private OrderStatusResponse transition(
      Long orderId, OrderTransition transition, Long actorId, ActorRole role) {
    ensureRoleOwnsTransition(transition, role);
    ensureActorOwnsOrder(orderId, actorId, role);

    applyTransition(orderId, transition);
    recordHistory(orderId, transition, role);

    return orderStatusMapper.toResponse(orderId, transition.to());
  }

  private void ensureReasonIsAvailableTo(ActorRole role, RejectionReason reason) {
    if (reason.isSystemOnly() && role != ActorRole.SYSTEM) {
      throw new ReservedRejectionReasonException(reason + " is reserved for the system");
    }
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
      case CUSTOMER -> {
        Optional<OrderOwnerProjection> owner = orderRepository.findOwnerById(orderId);
        ensureOwnerMatches(
            owner.map(OrderOwnerProjection::getCustomerId),
            actorId,
            "Order belongs to another customer");
        // Runs after the ownership check, so the owner is the caller.
        owner.ifPresent(o -> orderService.ensureOwnerActive(o.getCustomerDeletedAt()));
      }
      case COURIER -> {
        ensureOrderExists(orderId);
        throw new OrderNotOwnedException("No courier is assigned to orders yet");
      }
      // Unreachable today: no OrderTransition lists ADMIN, so ensureRoleOwnsTransition refuses it
      // first. Without this case, adding ADMIN to a transition would skip the ownership check.
      case ADMIN -> throw new OrderNotOwnedException("No ownership rule for an admin yet");
    }
  }

  private void ensureOrderExists(Long orderId) {
    if (orderRepository.findRestaurantIdById(orderId).isEmpty()) {
      throw new OrderNotFoundException("Order not found");
    }
  }

  private void ensureOwnerMatches(Optional<Long> owner, Long actorId, String message) {
    Long ownerId = owner.orElseThrow(() -> new OrderNotFoundException("Order not found"));
    orderService.ensureOwnedBy(ownerId, actorId, message);
  }

  private void applyTransition(Long orderId, OrderTransition transition) {
    int updatedRows =
        orderRepository.updateStatusIfCurrent(orderId, transition.from(), transition.to());
    if (updatedRows == 0) {
      throw new IllegalOrderTransitionException("Order is not in status " + transition.from());
    }
  }

  private void recordHistory(Long orderId, OrderTransition transition, ActorRole role) {
    orderStatusHistoryRepository.save(
        OrderStatusHistory.builder()
            .order(orderRepository.getReferenceById(orderId))
            .fromStatus(transition.from())
            .toStatus(transition.to())
            .actorRole(role)
            .at(OffsetDateTime.now())
            .build());
  }

  /** Puts every line's quantity back on its menu item. */
  private void restoreStock(Long orderId) {
    List<OrderLineProjection> lines = orderItemRepository.findLinesByOrderId(orderId);
    lines.forEach(line -> restaurantService.restoreStock(line.getMenuItemId(), line.getQuantity()));
  }
}
