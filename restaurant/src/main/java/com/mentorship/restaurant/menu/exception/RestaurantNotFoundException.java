package com.mentorship.restaurant.menu.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RestaurantNotFoundException extends RestaurantException {
  public RestaurantNotFoundException(String message) {
    super(message);
  }
}
