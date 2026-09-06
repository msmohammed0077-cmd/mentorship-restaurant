package com.mentorship.restaurant.cart.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class EmptyCartException extends CartException {

  public EmptyCartException(String message) {
    super(message);
  }
}
