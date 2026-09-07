package com.mentorship.restaurant.order.model.entity;

/**
 * The order vocabulary, frozen by GH-34. Eight statuses, no more — adding one is a change to the
 * umbrella, not to a sub-issue.
 *
 * <p>Two omissions are deliberate: there is no {@code OUT_FOR_DELIVERY} (indistinguishable from
 * {@code PICKED_UP}) and no {@code EXPIRED} (a 15-minute auto-reject writes {@code REJECTED} with a
 * system reason, so expiry and rejection share one cleanup path).
 */
public enum OrderStatus {
  PLACED,
  ACCEPTED,
  REJECTED,
  PREPARING,
  READY_FOR_PICKUP,
  PICKED_UP,
  DELIVERED,
  CANCELLED;

  /** Terminal statuses do not move. Nothing transitions out of these. */
  public boolean isTerminal() {
    return this == REJECTED || this == CANCELLED || this == DELIVERED;
  }
}
