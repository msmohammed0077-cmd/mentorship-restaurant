package com.mentorship.restaurant.order.service.handler;

import com.mentorship.restaurant.cart.exception.CustomerNotFoundException;
import com.mentorship.restaurant.cart.repository.CustomerRepository;
import com.mentorship.restaurant.order.exception.InvalidCursorException;
import com.mentorship.restaurant.order.exception.TransitionNotAllowedForRoleException;
import com.mentorship.restaurant.order.model.OrderCursor;
import com.mentorship.restaurant.order.model.entity.ActorRole;
import com.mentorship.restaurant.order.model.response.OrderHistoryResponse;
import com.mentorship.restaurant.order.model.response.OrderSummaryResponse;
import com.mentorship.restaurant.order.repository.OrderRepository;
import com.mentorship.restaurant.paging.KeysetPage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ViewOrderHistoryHandler {
  private static final int MIN_YEAR = 1;
  private static final int MAX_YEAR = 9999;

  private final CustomerRepository customerRepository;
  private final OrderRepository orderRepository;

  @Transactional(readOnly = true)
  public OrderHistoryResponse viewOrderHistory(
      Long customerId, ActorRole role, Integer limit, OrderCursor cursor) {
    ensureCustomerRole(role);
    ensureCustomerExists(customerId);
    ensureCursorIsUsable(cursor);

    PageRequest pageRequest = PageRequest.of(0, limit + 1);
    List<OrderSummaryResponse> rows =
        cursor == null || cursor.isAbsent()
            ? orderRepository.findFirstPage(customerId, pageRequest)
            : orderRepository.findPageAfter(
                customerId, cursor.getCreatedAt(), cursor.getOrderId(), pageRequest);

    KeysetPage<OrderSummaryResponse> page = KeysetPage.of(rows, limit);
    return new OrderHistoryResponse(page.items(), nextCursor(page));
  }

  private void ensureCustomerRole(ActorRole role) {
    if (role != ActorRole.CUSTOMER) {
      throw new TransitionNotAllowedForRoleException(
          "Role " + role + " may not read order history");
    }
  }

  private void ensureCustomerExists(Long customerId) {
    if (!customerRepository.existsById(customerId)) {
      throw new CustomerNotFoundException("Customer not found");
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

  private OrderCursor nextCursor(KeysetPage<OrderSummaryResponse> page) {
    if (!page.hasMore() || page.isEmpty()) {
      return null;
    }
    OrderSummaryResponse last = page.last();
    return new OrderCursor(last.getCreatedAt(), last.getOrderId());
  }
}
