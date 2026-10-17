package com.mentorship.restaurant.restaurant.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** A second menu: a restaurant has one (#93). */
@ResponseStatus(HttpStatus.CONFLICT)
public class MenuAlreadyExistsException extends RestaurantException {
  public MenuAlreadyExistsException(String message) {
    super(message);
  }
}
