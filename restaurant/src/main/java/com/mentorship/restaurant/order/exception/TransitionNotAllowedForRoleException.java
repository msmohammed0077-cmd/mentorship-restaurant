package com.mentorship.restaurant.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class TransitionNotAllowedForRoleException extends OrderException {

  public TransitionNotAllowedForRoleException(String message) {
    super(message);
  }
}
