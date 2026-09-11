package com.mentorship.restaurant.support;

import com.mentorship.restaurant.order.model.entity.Transaction;
import com.mentorship.restaurant.order.model.response.TransactionResponse;

public class PaymentProcesser {

    public Transaction process(String cardId) {
        return new Transaction();
    }
}
