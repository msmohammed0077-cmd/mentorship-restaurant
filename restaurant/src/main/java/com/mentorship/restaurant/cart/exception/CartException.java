package com.mentorship.restaurant.cart.exception;

/**
 * Base class for every cart exception. Subclasses declare their HTTP status with @ResponseStatus
 * and GlobalExceptionHandler reads it back, so the status is written down once, on the class it
 * belongs to. Extending this is the only thing a new cart exception has to do to be handled.
 */
public abstract class CartException extends RuntimeException {

  protected CartException(String message) {
    super(message);
  }
}
