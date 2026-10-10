package com.mentorship.restaurant.restaurant.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class RestaurantActionNotAllowedException extends RestaurantException {
  public RestaurantActionNotAllowedException(String message) {
    super(message);
  }
}
