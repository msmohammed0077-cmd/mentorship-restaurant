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

/**
 * The legal transitions. A Java enum rather than a database table: simplest, and an illegal
 * transition fails at compile time wherever the target is a constant. A {@code transitions} table
 * is what you reach for when the rules must change without a deploy — noted, not built.
 *
 * <p>{@link #PICK_UP} and {@link #DELIVER} are defined but reachable by no endpoint: courier
 * assignment is not ticketed anywhere, so the rules are complete and the HTTP surface deliberately
 * is not.
 */
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

  /**
   * SYSTEM may perform a rejection on the restaurant's behalf — the 15-minute auto-reject in GH-48.
   * It owns no transition of its own.
   */
  public boolean isPerformableBy(ActorRole role) {
    return role == owner || (role == ActorRole.SYSTEM && this == REJECT);
  }
}
