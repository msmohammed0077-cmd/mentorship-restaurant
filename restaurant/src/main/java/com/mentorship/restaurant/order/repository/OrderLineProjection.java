package com.mentorship.restaurant.order.repository;

/** What compensation needs from a line: which item, and how much to put back. */
public interface OrderLineProjection {

  Long getMenuItemId();

  Integer getQuantity();
}
