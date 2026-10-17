package com.mentorship.restaurant.restaurant.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** A restaurant-side request naming a restaurant the caller does not own. */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class RestaurantAccessDeniedException extends RestaurantException {

  public RestaurantAccessDeniedException(String message) {
    super(message);
  }
}
