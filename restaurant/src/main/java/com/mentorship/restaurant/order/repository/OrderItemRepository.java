package com.mentorship.restaurant.order.repository;

import com.mentorship.restaurant.order.model.entity.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

  @Query(
      """
      select orderItem.menuItem.id as menuItemId, orderItem.quantity as quantity
      from OrderItem orderItem where orderItem.order.id = :orderId
      """)
  List<OrderLineProjection> findLinesByOrderId(@Param("orderId") Long orderId);
}
