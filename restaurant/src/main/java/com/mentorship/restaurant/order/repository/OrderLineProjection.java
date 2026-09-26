package com.mentorship.restaurant.order.repository;

public interface OrderLineProjection {
  Long getMenuItemId();

  Integer getQuantity();
}
