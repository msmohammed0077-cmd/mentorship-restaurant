package com.mentorship.restaurant.order.model.mapper;

import com.mentorship.restaurant.order.model.response.OrderSummaryResponse;
import com.mentorship.restaurant.order.repository.OrderSummaryProjection;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

  public OrderSummaryResponse toSummary(OrderSummaryProjection projection) {
    return new OrderSummaryResponse(
        projection.getOrderId(),
        projection.getStatus(),
        projection.getRestaurantName(),
        projection.getItemCount(),
        projection.getTotal(),
        projection.getCreatedAt());
  }
}
