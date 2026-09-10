package com.mentorship.restaurant.order.model.entity;

import static com.mentorship.restaurant.order.model.entity.ActorRole.COURIER;
import static com.mentorship.restaurant.order.model.entity.ActorRole.CUSTOMER;
import static com.mentorship.restaurant.order.model.entity.ActorRole.RESTAURANT;
import static com.mentorship.restaurant.order.model.entity.OrderStatus.ACCEPTED;
import static com.mentorship.restaurant.order.model.entity.OrderStatus.CANCELLED;
import static com.mentorship.restaurant.order.model.entity.OrderStatus.DELIVERED;
import static com.mentorship.restaurant.order.model.entity.OrderStatus.PICKED_UP;
import static com.mentorship.restaurant.order.model.entity.OrderStatus.PLACED;
import static com.mentorship.restaurant.order.model.entity.OrderStatus.PREPARING;
import static com.mentorship.restaurant.order.model.entity.OrderStatus.READY_FOR_PICKUP;
import static com.mentorship.restaurant.order.model.entity.OrderStatus.REJECTED;

public enum OrderTransition {
  ACCEPT(PLACED, ACCEPTED, RESTAURANT),
  REJECT(PLACED, REJECTED, RESTAURANT),
  CANCEL(PLACED, CANCELLED, CUSTOMER),
  START_PREPARING(ACCEPTED, PREPARING, RESTAURANT),
  READY(PREPARING, READY_FOR_PICKUP, RESTAURANT),
  PICK_UP(READY_FOR_PICKUP, PICKED_UP, COURIER),
  DELIVER(PICKED_UP, DELIVERED, COURIER);

  private final OrderStatus from;
  private final OrderStatus to;
  private final ActorRole owner;

  OrderTransition(OrderStatus from, OrderStatus to, ActorRole owner) {
    this.from = from;
    this.to = to;
    this.owner = owner;
  }

  public OrderStatus from() {
    return from;
  }

  public OrderStatus to() {
    return to;
  }

  public ActorRole owner() {
    return owner;
  }

  public boolean isPerformableBy(ActorRole role) {
    return role == owner || (role == ActorRole.SYSTEM && this == REJECT);
  }
}
