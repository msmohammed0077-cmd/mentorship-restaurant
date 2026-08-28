package com.mentorship.restaurant.cart.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CartItemAlreadyExistsException extends RuntimeException {
  public CartItemAlreadyExistsException(String message) {
    super(message);
  }
}
