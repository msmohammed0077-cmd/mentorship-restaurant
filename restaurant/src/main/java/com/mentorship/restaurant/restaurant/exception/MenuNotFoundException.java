package com.mentorship.restaurant.restaurant.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** A restaurant with no menu, or whose menu is deleted. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class MenuNotFoundException extends RestaurantException {
  public MenuNotFoundException(String message) {
    super(message);
  }
}
