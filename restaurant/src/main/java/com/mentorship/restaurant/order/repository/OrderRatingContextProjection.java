package com.mentorship.restaurant.order.repository;

import com.mentorship.restaurant.order.model.entity.OrderStatus;
import java.time.OffsetDateTime;

/** What rate-order checks, read in one query. */
public interface OrderRatingContextProjection {
  Long getCustomerId();

  /** Null while the customer is active. */
  OffsetDateTime getCustomerDeletedAt();

  OrderStatus getStatus();

  Boolean getRated();
}
