package com.mentorship.restaurant.order.model.entity;

/**
 * Who is asking. There is no auth in this project: the controller takes this as a parameter and the
 * handler checks it against the transition's owner. Scoping, not authorisation — any caller may
 * pass any value.
 */
public enum ActorRole {
  CUSTOMER,
  RESTAURANT,
  COURIER,
  /** The 15-minute auto-reject in GH-48. Never supplied by a caller. */
  SYSTEM
}
