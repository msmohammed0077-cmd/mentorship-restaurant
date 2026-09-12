package com.mentorship.restaurant.order.model.mapper;

import com.mentorship.restaurant.order.model.entity.OrderItem;
import com.mentorship.restaurant.order.model.response.OrderItemResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderItemMapper {
  public List<OrderItemResponse> toResponse(List<OrderItem> orderItems) {
    return null;
  }

  public OrderItemResponse toItemResponse(OrderItem item) {
    BigDecimal totalPrice = item.getItemPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

    return OrderItemResponse.builder()
        .id(item.getId())
        .menuItemId(item.getMenuItem().getId())
        .itemName(item.getItemName())
        .quantity(item.getQuantity())
        .itemPrice(item.getItemPrice())
        .totalPrice(totalPrice)
        .build();
  }
}
