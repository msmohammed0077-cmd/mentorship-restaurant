package com.mentorship.restaurant.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class OrderQuantityIsNotAllowed extends OrderException {
  public OrderQuantityIsNotAllowed(String message) {
    super(message);
  }
}
