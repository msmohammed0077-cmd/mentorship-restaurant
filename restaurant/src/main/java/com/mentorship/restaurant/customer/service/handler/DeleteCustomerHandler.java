package com.mentorship.restaurant.customer.service.handler;

import com.mentorship.restaurant.cart.repository.CartRepository;
import com.mentorship.restaurant.customer.exception.CustomerHasActiveOrdersException;
import com.mentorship.restaurant.customer.exception.CustomerNotFoundException;
import com.mentorship.restaurant.customer.model.entity.Customer;
import com.mentorship.restaurant.customer.repository.CustomerRepository;
import com.mentorship.restaurant.order.model.entity.OrderStatus;
import com.mentorship.restaurant.order.repository.OrderRepository;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteCustomerHandler {

  /** Derived from {@link OrderStatus#isTerminal()} so the two can never disagree. */
  private static final Set<OrderStatus> ACTIVE_ORDER_STATUSES =
      Arrays.stream(OrderStatus.values())
          .filter(status -> !status.isTerminal())
          .collect(Collectors.toCollection(() -> EnumSet.noneOf(OrderStatus.class)));

  private final CustomerRepository customerRepository;
  private final OrderRepository orderRepository;
  private final CartRepository cartRepository;

  @Transactional
  public void deleteCustomer(Long customerId) {
    Customer customer =
        customerRepository
            .findActiveById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

    ensureNoActiveOrders(customerId);

    // Managed entity: dirty checking issues the UPDATE, no save() needed.
    customer.getUser().setUserDeletedAt(OffsetDateTime.now());

    // Bulk delete: flushAutomatically writes the soft-delete above first. Nothing is read after
    // it, so no stale entity can leak out. cart_items go with the cart by ON DELETE CASCADE.
    cartRepository.deleteByCustomer_Id(customerId);
  }

  /** A restaurant must never lose the customer of an order still in flight. */
  private void ensureNoActiveOrders(Long customerId) {
    if (orderRepository.existsByCustomer_IdAndStatusIn(customerId, ACTIVE_ORDER_STATUSES)) {
      throw new CustomerHasActiveOrdersException("Customer has active orders");
    }
  }
}
