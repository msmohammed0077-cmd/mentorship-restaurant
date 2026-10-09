package com.mentorship.restaurant.order.repository;

import com.mentorship.restaurant.order.model.entity.OrderItem;
import java.util.Collection;
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

  /**
   * Line counts for a page of orders, in one grouped query. An order with no lines has no row; the
   * caller reads that as 0.
   */
  @Query(
      """
      select orderItem.order.id as orderId, count(orderItem) as itemCount
      from OrderItem orderItem where orderItem.order.id in :orderIds
      group by orderItem.order.id
      """)
  List<OrderItemCountProjection> countItemsByOrderIds(@Param("orderIds") Collection<Long> orderIds);
}
