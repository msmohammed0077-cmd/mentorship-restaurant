package com.mentorship.restaurant.restaurant.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class RestaurantClosedException extends RestaurantException {
  public RestaurantClosedException(String message) {
    super(message);
  }
}
