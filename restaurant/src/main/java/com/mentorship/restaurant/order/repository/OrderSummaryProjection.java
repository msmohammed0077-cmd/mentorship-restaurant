package com.mentorship.restaurant.order.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * A history row. A projection rather than the entity: the list shows a count of lines, so loading
 * them would be work thrown away.
 */
public interface OrderSummaryProjection {

  Long getOrderId();

  String getStatus();

  String getRestaurantName();

  Long getItemCount();

  BigDecimal getTotal();

  OffsetDateTime getCreatedAt();
}
