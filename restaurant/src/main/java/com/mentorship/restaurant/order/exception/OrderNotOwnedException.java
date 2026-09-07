package com.mentorship.restaurant.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class OrderNotOwnedException extends OrderException {

  public OrderNotOwnedException(String message) {
    super(message);
  }
}
