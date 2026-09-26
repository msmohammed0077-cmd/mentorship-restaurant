package com.mentorship.restaurant.order.service.handler.createOrder;

import com.mentorship.restaurant.order.model.request.CreateOrderRequest;
import com.mentorship.restaurant.order.model.response.OrderResponse;

public abstract class OrderHandler {

    protected OrderHandler next;

    public static OrderHandler processOrder(OrderHandler first, OrderHandler... chain) {
        OrderHandler head = first;
        for (OrderHandler nextInChain : chain) {
            head.next = nextInChain;
            head = nextInChain;
        }

        return first;
    }

    public abstract OrderResponse handle(CreateOrderRequest request, OrderResponse response);

    protected OrderResponse handleNext(CreateOrderRequest request, OrderResponse response) {
        if (next == null) {
            return response;
        }

        return next.handleNext(request, response);
    }

}
