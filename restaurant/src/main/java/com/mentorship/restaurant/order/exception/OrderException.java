package com.mentorship.restaurant.order.exception;

/**
 * Base for order exceptions, mirroring CartException. GlobalExceptionHandler reads the status back
 * off @ResponseStatus, so extending this is the only registration step. Collapsing this and
 * CartException into one base is its own ticket.
 */
public abstract class OrderException extends RuntimeException {

  protected OrderException(String message) {
    super(message);
  }
}
