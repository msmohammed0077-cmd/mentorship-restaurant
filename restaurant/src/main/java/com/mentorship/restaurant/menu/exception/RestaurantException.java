package com.mentorship.restaurant.menu.exception;

public abstract class RestaurantException extends RuntimeException {
  protected RestaurantException(String message) {
    super(message);
  }
}
