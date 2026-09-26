package com.mentorship.restaurant.order.repository;

import com.mentorship.restaurant.order.model.entity.OrderRating;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRatingRepository extends JpaRepository<OrderRating, Long> {
  boolean existsByOrderId(Long orderId);
}
