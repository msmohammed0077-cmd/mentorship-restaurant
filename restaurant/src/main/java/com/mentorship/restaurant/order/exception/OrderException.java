package com.mentorship.restaurant.order.exception;

public abstract class OrderException extends RuntimeException {
  protected OrderException(String message) {
    super(message);
  }
}
