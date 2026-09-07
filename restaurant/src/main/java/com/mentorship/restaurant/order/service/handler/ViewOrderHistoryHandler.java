package com.mentorship.restaurant.order.service.handler;

import com.mentorship.restaurant.cart.exception.CustomerNotFoundException;
import com.mentorship.restaurant.cart.repository.CustomerRepository;
import com.mentorship.restaurant.order.model.mapper.OrderMapper;
import com.mentorship.restaurant.order.model.response.OrderHistoryResponse;
import com.mentorship.restaurant.order.model.response.OrderSummaryResponse;
import com.mentorship.restaurant.order.repository.OrderRepository;
import com.mentorship.restaurant.order.repository.OrderSummaryProjection;
import com.mentorship.restaurant.order.service.OrderCursor;
import com.mentorship.restaurant.permission.PermissionDeniedException;
import com.mentorship.restaurant.permission.PermissionResolver;
import com.mentorship.restaurant.permission.Permissions;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ViewOrderHistoryHandler {

  private final CustomerRepository customerRepository;
  private final OrderRepository orderRepository;
  private final PermissionResolver permissionResolver;
  private final OrderMapper orderMapper;

  @Transactional(readOnly = true)
  public OrderHistoryResponse viewOrderHistory(Long customerId, Integer limit, String cursor) {
    Long userId = ensureCustomerExists(customerId);
    ensureMayReadOrders(userId);

    // One extra row tells us whether another page exists, without a count query.
    PageRequest pageRequest = PageRequest.of(0, limit + 1);
    List<OrderSummaryProjection> page =
        cursor == null
            ? orderRepository.findFirstPage(customerId, pageRequest)
            : pageAfter(customerId, OrderCursor.decode(cursor), pageRequest);

    boolean hasMore = page.size() > limit;
    List<OrderSummaryProjection> visible = hasMore ? page.subList(0, limit) : page;

    List<OrderSummaryResponse> orders = new ArrayList<>(visible.size());
    visible.forEach(projection -> orders.add(orderMapper.toSummary(projection)));

    return new OrderHistoryResponse(orders, nextCursor(visible, hasMore));
  }

  private List<OrderSummaryProjection> pageAfter(
      Long customerId, OrderCursor position, PageRequest pageRequest) {
    return orderRepository.findPageAfter(
        customerId, position.createdAt(), position.orderId(), pageRequest);
  }

  private Long ensureCustomerExists(Long customerId) {
    return customerRepository
        .findUserIdById(customerId)
        .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
  }

  private void ensureMayReadOrders(Long userId) {
    if (!permissionResolver.permissionsOf(userId).contains(Permissions.ORDERS_READ_ORDER)) {
      throw new PermissionDeniedException("Missing permission: " + Permissions.ORDERS_READ_ORDER);
    }
  }

  private String nextCursor(List<OrderSummaryProjection> visible, boolean hasMore) {
    if (!hasMore || visible.isEmpty()) {
      return null;
    }
    OrderSummaryProjection last = visible.get(visible.size() - 1);
    return OrderCursor.encode(last.getCreatedAt(), last.getOrderId());
  }
}
