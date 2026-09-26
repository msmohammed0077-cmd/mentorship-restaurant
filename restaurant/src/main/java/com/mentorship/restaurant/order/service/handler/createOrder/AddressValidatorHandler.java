package com.mentorship.restaurant.order.service.handler.createOrder;

import com.mentorship.restaurant.customer.exception.AddressNotFoundException;
import com.mentorship.restaurant.customer.model.entity.Address;
import com.mentorship.restaurant.order.model.request.CreateOrderRequest;
import com.mentorship.restaurant.order.model.response.OrderResponse;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AddressValidatorHandler extends OrderHandler {

    private final Address address;

    @Override
    public OrderResponse handle(CreateOrderRequest request, OrderResponse response) {

        Long customerId = request.getCustomerId();

        Long customerIdFromAddress = address.getCustomer().getId();

        if (!customerIdFromAddress.equals(customerId)) {
            throw new AddressNotFoundException("Address does not belong to customer");
        }

        return handleNext(request, response);
    }
}
