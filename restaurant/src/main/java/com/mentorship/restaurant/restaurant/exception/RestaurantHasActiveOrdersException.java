package com.mentorship.restaurant.restaurant.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class RestaurantHasActiveOrdersException extends RestaurantException {
  public RestaurantHasActiveOrdersException(String message) {
    super(message);
  }
}
