package com.mentorship.restaurant.user.model;

/**
 * Who a caller claims to be. Trust-based until real authentication (#83): endpoints take it as
 * {@code ?role=} and services check it.
 */
public enum ActorRole {
  CUSTOMER,
  RESTAURANT,
  COURIER,
  SYSTEM,
  ADMIN
}
