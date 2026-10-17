package com.mentorship.restaurant.restaurant.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class RestaurantEmailInUseException extends RestaurantException {
  public RestaurantEmailInUseException(String message) {
    super(message);
  }
}
