package com.mentorship.restaurant.order.service.handler;

import com.mentorship.restaurant.order.exception.OrderNotFoundException;
import com.mentorship.restaurant.order.model.entity.Order;
import com.mentorship.restaurant.order.model.mapper.OrderMapper;
import com.mentorship.restaurant.order.model.response.OrderResponse;
import com.mentorship.restaurant.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ViewOrderHandler {

  private final OrderRepository orderRepository;
  private final OrderMapper orderMapper;

  @Transactional(readOnly = true)
  public OrderResponse viewCart(Long orderId) {
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found"));

    return orderMapper.toResponse(order);
  }
}
