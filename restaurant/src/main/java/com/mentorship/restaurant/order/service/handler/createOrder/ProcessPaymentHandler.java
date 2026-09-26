package com.mentorship.restaurant.order.service.handler.createOrder;

import com.mentorship.restaurant.order.model.entity.Transaction;
import com.mentorship.restaurant.order.model.request.CreateOrderRequest;
import com.mentorship.restaurant.order.model.request.PaymentMethod;
import com.mentorship.restaurant.order.model.response.OrderResponse;
import com.mentorship.restaurant.support.PaymentProcesser;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ProcessPaymentHandler extends OrderHandler {

    private final PaymentProcesser paymentProcesser;

    @Override
    public OrderResponse handle(CreateOrderRequest request, OrderResponse response) {


        if (PaymentMethod.CARD.equals(request.getPaymentMethod())) {
            Transaction transaction = paymentProcesser.process(request.getCardId());
            response.setTransaction(transaction);
        }

        return handleNext(request, response);
    }
}
