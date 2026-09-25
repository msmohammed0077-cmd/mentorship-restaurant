package com.mentorship.restaurant.customer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PaymentMethodExpiredException extends CustomerException {

  public PaymentMethodExpiredException(String message) {
    super(message);
  }
}
