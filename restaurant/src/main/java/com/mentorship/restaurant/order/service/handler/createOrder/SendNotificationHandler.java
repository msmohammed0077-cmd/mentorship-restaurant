package com.mentorship.restaurant.order.service.handler.createOrder;

import com.mentorship.restaurant.order.model.request.CreateOrderRequest;
import com.mentorship.restaurant.order.model.response.OrderResponse;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SendNotificationHandler extends OrderHandler {


    @Override
    public OrderResponse handle(CreateOrderRequest request, OrderResponse response) {
        //imp
        return handleNext(request, response);
    }
}
