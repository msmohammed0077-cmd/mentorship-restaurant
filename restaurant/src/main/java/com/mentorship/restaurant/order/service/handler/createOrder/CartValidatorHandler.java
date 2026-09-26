package com.mentorship.restaurant.order.service.handler.createOrder;

import ch.qos.logback.core.joran.conditional.IfAction;
import com.mentorship.restaurant.cart.exception.CartNotFoundException;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.order.model.request.CreateOrderRequest;
import com.mentorship.restaurant.order.model.response.OrderResponse;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CartValidatorHandler extends OrderHandler {

    private final Cart cart;

    @Override
    public OrderResponse handle(CreateOrderRequest request, OrderResponse response) {

        Long requestCustomerId = request.getCustomerId();
        Long cartCustomerId = cart.getCustomer().getId();

        if (!cartCustomerId.equals(requestCustomerId)) {
            throw new CartNotFoundException("cart does not belong to customer");
        }

        return handleNext(request, response);
    }
}
