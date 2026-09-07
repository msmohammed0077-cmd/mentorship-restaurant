package com.mentorship.restaurant.order.repository;

import com.mentorship.restaurant.order.model.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {}
