package com.mentorship.restaurant.customer.service;

import com.mentorship.restaurant.customer.exception.CustomerHasActiveOrdersException;
import com.mentorship.restaurant.customer.model.entity.Customer;
import com.mentorship.restaurant.order.service.OrderService;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Delete-customer, on its own because it is the only customer use-case that needs the order domain,
 * and order (through cart and address) needs {@link CustomerService}. Inside CustomerService it
 * would close a constructor-injection cycle; here nothing points back at it. ADR 0004.
 */
@Service
@RequiredArgsConstructor
public class CustomerDeletionService {

  private final CustomerService customerService;
  private final OrderService orderService;

  /** A soft delete. Past orders, ratings, addresses and the cart are kept (#104). */
  @Transactional
  public void deleteCustomer(Long customerId) {
    Customer customer = customerService.findActiveCustomer(customerId);

    ensureNoActiveOrders(customerId);

    // Managed entity: dirty checking issues the UPDATE, no save() needed.
    customer.getUser().setUserDeletedAt(OffsetDateTime.now());
  }

  /** A restaurant must never lose the customer of an order still in flight. */
  private void ensureNoActiveOrders(Long customerId) {
    if (orderService.hasActiveOrders(customerId)) {
      throw new CustomerHasActiveOrdersException("Customer has active orders");
    }
  }
}
