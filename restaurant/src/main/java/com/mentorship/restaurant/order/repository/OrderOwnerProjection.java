package com.mentorship.restaurant.order.repository;

import java.time.OffsetDateTime;

public interface OrderOwnerProjection {
  Long getCustomerId();

  /** Null while the customer is active. */
  OffsetDateTime getCustomerDeletedAt();
}
