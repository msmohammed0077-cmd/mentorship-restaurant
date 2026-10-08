package com.mentorship.restaurant.payment.service;

import com.mentorship.restaurant.payment.model.entity.Transaction;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class PaymentProcessor {

  public Transaction process(String cardId) {
    return Transaction.builder().status("PAID").transactionDate(LocalDateTime.now()).build();
  }
}
