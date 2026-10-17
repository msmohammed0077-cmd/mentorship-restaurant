package com.mentorship.restaurant.user.model;

import java.time.OffsetDateTime;

/**
 * What resolving a token reads in one query: the {@link Caller}, plus the stored email and
 * soft-delete marker that keep our copy of the email in step with Keycloak's.
 */
public record CallerRow(Long userId, Long customerId, String email, OffsetDateTime deletedAt) {

  public Caller caller() {
    return new Caller(userId, customerId);
  }
}
