package com.mentorship.restaurant.cart.exception;

public abstract class CartException extends RuntimeException {

  protected CartException(String message) {
    super(message);
  }
}
