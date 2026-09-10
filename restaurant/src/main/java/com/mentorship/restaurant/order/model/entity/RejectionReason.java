package com.mentorship.restaurant.order.model.entity;

public enum RejectionReason {
  OUT_OF_INGREDIENTS,
  TOO_BUSY,
  CLOSING_SOON,
  OTHER,
  NO_RESPONSE;

  public boolean isSystemOnly() {
    return this == NO_RESPONSE;
  }
}
