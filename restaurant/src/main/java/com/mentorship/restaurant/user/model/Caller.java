package com.mentorship.restaurant.user.model;

import com.mentorship.restaurant.customer.exception.NotACustomerException;

/**
 * Who sent the request, resolved from the token's {@code sub} after verification (ADR 0009).
 * {@code customerId} is null when the user is not a customer.
 */
public record Caller(Long userId, Long customerId) {

  /** The caller's customer id, else a 403: the admin and restaurant owners are not customers. */
  public Long requireCustomerId() {
    if (customerId == null) {
      throw new NotACustomerException("Caller is not a customer");
    }
    return customerId;
  }
}
