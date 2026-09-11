package com.mentorship.restaurant.order.repository;

import com.mentorship.restaurant.order.model.entity.OrderTracking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderTrackingRepository extends JpaRepository<OrderTracking, Long> {
}