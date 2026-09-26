package com.mentorship.restaurant.support;

import java.time.OffsetDateTime;
import java.time.YearMonth;

/**
 * Payment method tests use customers they insert themselves, never the seeded ones. Cards a test
 * needs as setup are inserted straight into the database, so only the endpoint under test is called
 * over HTTP.
 */
public abstract class PaymentMethodEndpointTestSupport extends CustomerEndpointTestSupport {

  /** Valid through the end of its month, so "now plus two years" is always in date. */
  protected static final YearMonth VALID_EXPIRY = YearMonth.now().plusYears(2);

  protected static final YearMonth EXPIRED = YearMonth.now().minusMonths(1);

  /** Inserts a valid VISA card created now. Returns its id. */
  protected long insertPaymentMethod(long customerId, String last4, boolean isDefault) {
    return insertPaymentMethod(customerId, last4, isDefault, OffsetDateTime.now());
  }

  /** Inserts a valid VISA card created at {@code createdAt}. Returns its id. */
  protected long insertPaymentMethod(
      long customerId, String last4, boolean isDefault, OffsetDateTime createdAt) {
    Long paymentMethodId =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO payment_methods (
              customer_id, payment_method_brand, payment_method_last4,
              payment_method_expiry_month, payment_method_expiry_year,
              payment_method_holder_name, payment_method_is_default, payment_method_created_at
            )
            VALUES (?, 'VISA', ?, ?, ?, 'Sara Youssef', ?, ?)
            RETURNING payment_method_id
            """,
            Long.class,
            customerId,
            last4,
            VALID_EXPIRY.getMonthValue(),
            VALID_EXPIRY.getYear(),
            isDefault,
            createdAt);
    if (paymentMethodId == null) {
      throw new IllegalStateException("Payment method not created for customer " + customerId);
    }
    return paymentMethodId;
  }

  protected int paymentMethodCountFor(long customerId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM payment_methods WHERE customer_id = ?",
            Integer.class,
            customerId);
    return count == null ? 0 : count;
  }

  protected boolean isDefault(long paymentMethodId) {
    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject(
            "SELECT payment_method_is_default FROM payment_methods WHERE payment_method_id = ?",
            Boolean.class,
            paymentMethodId));
  }
}
