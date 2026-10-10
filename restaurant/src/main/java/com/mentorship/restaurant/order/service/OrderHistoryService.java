package com.mentorship.restaurant.order.service;

import com.mentorship.restaurant.customer.service.CustomerService;
import com.mentorship.restaurant.order.exception.InvalidCursorException;
import com.mentorship.restaurant.order.exception.TransitionNotAllowedForRoleException;
import com.mentorship.restaurant.order.model.OrderCursor;
import com.mentorship.restaurant.order.model.entity.Order;
import com.mentorship.restaurant.order.model.mapper.OrderMapper;
import com.mentorship.restaurant.order.model.response.OrderHistoryResponse;
import com.mentorship.restaurant.order.model.response.OrderSummaryResponse;
import com.mentorship.restaurant.order.repository.OrderItemCountProjection;
import com.mentorship.restaurant.order.repository.OrderItemRepository;
import com.mentorship.restaurant.order.repository.OrderRepository;
import com.mentorship.restaurant.user.model.ActorRole;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderHistoryService {

  private static final int MIN_YEAR = 1;
  private static final int MAX_YEAR = 9999;

  private final CustomerService customerService;
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final OrderMapper orderMapper;

  @Transactional(readOnly = true)
  public OrderHistoryResponse viewOrderHistory(
      Long customerId, ActorRole role, Integer limit, OrderCursor cursor) {
    ensureCustomerRole(role);
    customerService.ensureActiveCustomerExists(customerId);
    ensureCursorIsUsable(cursor);

    Window<Order> window =
        orderRepository.findByCustomer_IdOrderByCreatedAtDescIdDesc(
            customerId, positionOf(cursor), Limit.of(limit));
    Map<Long, Long> itemCounts = itemCountsOf(window.getContent());

    List<OrderSummaryResponse> rows =
        window.getContent().stream()
            .map(order -> orderMapper.toSummary(order, itemCounts.getOrDefault(order.getId(), 0L)))
            .toList();
    return new OrderHistoryResponse(rows, nextCursor(window));
  }

  private void ensureCustomerRole(ActorRole role) {
    if (role != ActorRole.CUSTOMER) {
      throw new TransitionNotAllowedForRoleException(
          "Role " + role + " may not read order history");
    }
  }

  private void ensureCursorIsUsable(OrderCursor cursor) {
    if (cursor == null || cursor.isAbsent()) {
      return;
    }
    if (cursor.isPartial()) {
      throw new InvalidCursorException("Cursor needs both createdAt and orderId");
    }
    int year = cursor.getCreatedAt().getYear();
    if (year < MIN_YEAR || year > MAX_YEAR) {
      throw new InvalidCursorException("Cursor timestamp is out of range");
    }
  }

  /** No cursor starts at the newest order; a cursor continues after the row it names. */
  private ScrollPosition positionOf(OrderCursor cursor) {
    if (cursor == null || cursor.isAbsent()) {
      return ScrollPosition.keyset();
    }
    return ScrollPosition.forward(
        Map.of("createdAt", cursor.getCreatedAt(), "id", cursor.getOrderId()));
  }

  /** One grouped query for the page, not one count per order. */
  private Map<Long, Long> itemCountsOf(List<Order> orders) {
    if (orders.isEmpty()) {
      return Map.of();
    }
    List<Long> orderIds = orders.stream().map(Order::getId).toList();
    return orderItemRepository.countItemsByOrderIds(orderIds).stream()
        .collect(
            Collectors.toMap(
                OrderItemCountProjection::getOrderId, OrderItemCountProjection::getItemCount));
  }

  /** The last row's keys, while Spring found another page; absent on the last page. */
  private OrderCursor nextCursor(Window<Order> window) {
    if (!window.hasNext() || window.isEmpty()) {
      return null;
    }
    Order last = window.getContent().get(window.size() - 1);
    return new OrderCursor(last.getCreatedAt(), last.getId());
  }
}
