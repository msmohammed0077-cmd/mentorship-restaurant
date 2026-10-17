package com.mentorship.restaurant.customer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** A customer-side endpoint called by a user who is not a customer, such as the admin. */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class NotACustomerException extends CustomerException {

  public NotACustomerException(String message) {
    super(message);
  }
}
