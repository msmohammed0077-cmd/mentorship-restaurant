package com.mentorship.restaurant.order.model.entity;

public enum OrderStatus {
  PLACED,
  ACCEPTED,
  REJECTED,
  PREPARING,
  READY_FOR_PICKUP,
  PICKED_UP,
  DELIVERED,
  CANCELLED;

  public boolean isTerminal() {
    return this == REJECTED || this == CANCELLED || this == DELIVERED;
  }
}
