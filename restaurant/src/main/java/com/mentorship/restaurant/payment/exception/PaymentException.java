package com.mentorship.restaurant.payment.exception;

public abstract class PaymentException extends RuntimeException {

  protected PaymentException(String message) {
    super(message);
  }
}
