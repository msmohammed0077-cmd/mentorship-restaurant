package com.mentorship.restaurant.restaurant.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class OutOfStockException extends RestaurantException {

  public OutOfStockException(String message) {
    super(message);
  }
}
