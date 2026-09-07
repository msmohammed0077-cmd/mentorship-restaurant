package com.mentorship.restaurant.order.model.entity;

/**
 * A fixed set plus an optional free-text note: the set is what the customer sees and what you can
 * report on, the note covers what the set does not.
 */
public enum RejectionReason {
  OUT_OF_INGREDIENTS,
  TOO_BUSY,
  CLOSING_SOON,
  OTHER,
  /**
   * The 15-minute auto-reject. System-only: reusing OTHER would make an unanswered order
   * indistinguishable from a restaurant that chose not to explain itself, which is exactly the
   * thing worth reporting on.
   */
  NO_RESPONSE;

  public boolean isSystemOnly() {
    return this == NO_RESPONSE;
  }
}
