package com.mentorship.restaurant.restaurant.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class MenuCodeInUseException extends RestaurantException {
  public MenuCodeInUseException(String message) {
    super(message);
  }
}
