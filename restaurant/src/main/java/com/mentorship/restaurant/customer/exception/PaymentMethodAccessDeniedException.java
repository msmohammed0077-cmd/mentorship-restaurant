package com.mentorship.restaurant.customer.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class PaymentMethodAccessDeniedException extends CustomerException {

  public PaymentMethodAccessDeniedException(String message) {
    super(message);
  }
}
