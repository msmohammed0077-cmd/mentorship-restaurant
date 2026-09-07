package com.mentorship.restaurant.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class IllegalOrderTransitionException extends OrderException {

  public IllegalOrderTransitionException(String message) {
    super(message);
  }
}
