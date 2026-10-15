package com.mentorship.restaurant.order.service.createorder;

import com.mentorship.restaurant.cart.exception.CartNotFoundException;
import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.order.model.request.CreateOrderRequest;
import com.mentorship.restaurant.order.model.response.OrderResponse;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CartValidatorHandler extends OrderHandler {

  private final Cart cart;
  private final Long customerId;

  @Override
  public OrderResponse handle(CreateOrderRequest request, OrderResponse response) {

    Long cartCustomerId = cart.getCustomer().getId();

    if (!cartCustomerId.equals(customerId)) {
      throw new CartNotFoundException("cart does not belong to customer");
    }

    return handleNext(request, response);
  }
}
