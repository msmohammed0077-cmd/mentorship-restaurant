package com.mentorship.restaurant.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidCursorException extends OrderException {

  public InvalidCursorException(String message) {
    super(message);
  }
}
