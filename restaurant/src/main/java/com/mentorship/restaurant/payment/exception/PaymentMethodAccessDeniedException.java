package com.mentorship.restaurant.payment.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class PaymentMethodAccessDeniedException extends PaymentException {

  public PaymentMethodAccessDeniedException(String message) {
    super(message);
  }
}
