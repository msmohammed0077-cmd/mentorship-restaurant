package com.mentorship.restaurant.order.model.mapper;

import com.mentorship.restaurant.order.model.entity.OrderRating;
import com.mentorship.restaurant.order.model.response.OrderRatingResponse;
import org.springframework.stereotype.Component;

@Component
public class OrderRatingMapper {
  public OrderRatingResponse toResponse(OrderRating rating) {
    return new OrderRatingResponse(
        rating.getId(),
        rating.getOrder().getId(),
        rating.getScore(),
        rating.getComment(),
        rating.getCreatedAt());
  }
}
