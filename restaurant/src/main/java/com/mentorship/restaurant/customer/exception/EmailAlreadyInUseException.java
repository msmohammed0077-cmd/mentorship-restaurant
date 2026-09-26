package com.mentorship.restaurant.customer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class EmailAlreadyInUseException extends CustomerException {

  public EmailAlreadyInUseException(String message) {
    super(message);
  }
}
