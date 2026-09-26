package com.mentorship.restaurant.support;

import com.mentorship.restaurant.order.model.entity.Transaction;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class PaymentProcesser {

  public Transaction process(String cardId) {
    Transaction transaction = new Transaction();
    transaction.setStatus("PAID");
    transaction.setTransactionDate(LocalDateTime.now());
    return transaction;
  }
}
