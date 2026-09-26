package com.mentorship.restaurant.cart.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DifferentRestaurantException extends CartException {
  public DifferentRestaurantException(String message) {
    super(message);
  }
}
