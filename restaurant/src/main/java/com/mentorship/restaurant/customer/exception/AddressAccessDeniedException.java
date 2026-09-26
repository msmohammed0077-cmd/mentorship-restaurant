package com.mentorship.restaurant.customer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class AddressAccessDeniedException extends CustomerException {

  public AddressAccessDeniedException(String message) {
    super(message);
  }
}
