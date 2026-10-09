package com.mentorship.restaurant.order.model.entity;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum OrderStatus {
  PLACED,
  ACCEPTED,
  REJECTED,
  PREPARING,
  READY_FOR_PICKUP,
  PICKED_UP,
  DELIVERED,
  CANCELLED;

  /** Every non-terminal status, derived from {@link #isTerminal()} so the two never disagree. */
  public static final Set<OrderStatus> ACTIVE =
      Collections.unmodifiableSet(
          Arrays.stream(values())
              .filter(status -> !status.isTerminal())
              .collect(Collectors.toCollection(() -> EnumSet.noneOf(OrderStatus.class))));

  public boolean isTerminal() {
    return this == REJECTED || this == CANCELLED || this == DELIVERED;
  }
}
