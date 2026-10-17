package com.mentorship.restaurant.restaurant.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** An item its owner has taken off: it cannot be carted or ordered. */
@ResponseStatus(HttpStatus.CONFLICT)
public class MenuItemUnavailableException extends RestaurantException {
  public MenuItemUnavailableException(String message) {
    super(message);
  }
}
