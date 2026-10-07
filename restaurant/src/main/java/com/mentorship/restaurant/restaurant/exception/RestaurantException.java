package com.mentorship.restaurant.restaurant.exception;

public abstract class RestaurantException extends RuntimeException {

  protected RestaurantException(String message) {
    super(message);
  }
}
