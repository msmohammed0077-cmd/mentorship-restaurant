package com.mentorship.restaurant.order.repository;

public interface OrderItemCountProjection {
  Long getOrderId();

  Long getItemCount();
}
