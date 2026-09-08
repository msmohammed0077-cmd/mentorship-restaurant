package com.mentorship.restaurant.order.model.mapper;

import com.mentorship.restaurant.order.model.entity.OrderStatus;
import com.mentorship.restaurant.order.model.response.OrderStatusResponse;
import org.springframework.stereotype.Component;

@Component
public class OrderStatusMapper {
  public OrderStatusResponse toResponse(Long orderId, OrderStatus status) {
    return new OrderStatusResponse(orderId, status.name());
  }
}
