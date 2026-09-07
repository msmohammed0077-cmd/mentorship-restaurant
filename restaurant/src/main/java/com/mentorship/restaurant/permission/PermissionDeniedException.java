package com.mentorship.restaurant.permission;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Extends RuntimeException rather than an order or cart base, so it must be registered explicitly
 * in GlobalExceptionHandler — otherwise it falls through to the generic handler and becomes a 500.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class PermissionDeniedException extends RuntimeException {

  public PermissionDeniedException(String message) {
    super(message);
  }
}
