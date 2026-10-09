package com.mentorship.restaurant.order.service.createorder;

import com.mentorship.restaurant.order.model.request.CreateOrderRequest;
import com.mentorship.restaurant.order.model.request.PaymentType;
import com.mentorship.restaurant.order.model.response.OrderResponse;
import com.mentorship.restaurant.payment.model.entity.Transaction;
import com.mentorship.restaurant.payment.service.PaymentProcessor;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ProcessPaymentHandler extends OrderHandler {

  private final PaymentProcessor paymentProcessor;

  @Override
  public OrderResponse handle(CreateOrderRequest request, OrderResponse response) {

    if (PaymentType.CARD.equals(request.getPaymentMethod())) {
      Transaction transaction = paymentProcessor.process(request.getCardId());
      response.setTransaction(transaction);
    }

    return handleNext(request, response);
  }
}
