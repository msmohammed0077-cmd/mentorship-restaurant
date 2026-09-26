package com.mentorship.restaurant.order.service.handler.createOrder;

import com.mentorship.restaurant.cart.model.entity.Cart;
import com.mentorship.restaurant.cart.repository.CartRepository;
import com.mentorship.restaurant.customer.model.entity.Address;
import com.mentorship.restaurant.order.model.entity.Order;
import com.mentorship.restaurant.order.model.mapper.OrderMapper;
import com.mentorship.restaurant.order.model.request.CreateOrderRequest;
import com.mentorship.restaurant.order.model.response.OrderResponse;
import com.mentorship.restaurant.order.repository.OrderRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class OrderFinalizer extends OrderHandler {


    private final Cart cart;
    private final Address address;
    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;

    @Override
    public OrderResponse handle(CreateOrderRequest request, OrderResponse response) {

        Order order = orderMapper.createNewOrderEntity(request, cart, address, response.getTransaction());

        Order saved = orderRepository.save(order);

        cartRepository.deleteById(request.getCartId());

        response = orderMapper.toResponse(saved);

        return handleNext(request, response);
    }
}
