package com.mentorship.restaurant.customer.exception;

public abstract class CustomerException extends RuntimeException {

  protected CustomerException(String message) {
    super(message);
  }
}
