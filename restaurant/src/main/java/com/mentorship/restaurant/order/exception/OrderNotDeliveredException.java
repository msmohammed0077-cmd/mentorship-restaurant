package com.mentorship.restaurant.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class OrderNotDeliveredException extends OrderException {
  public OrderNotDeliveredException(String message) {
    super(message);
  }
}
