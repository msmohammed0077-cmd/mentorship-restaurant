package com.mentorship.restaurant.order.repository;

import com.mentorship.restaurant.order.model.entity.Order;
import com.mentorship.restaurant.order.model.entity.OrderStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
  @Modifying(flushAutomatically = true)
  @Query(
      """
      update Order o set o.status = :to
      where o.id = :orderId and o.status = :from
      """)
  int updateStatusIfCurrent(
      @Param("orderId") Long orderId, @Param("from") OrderStatus from, @Param("to") OrderStatus to);

  @Query("select o.restaurant.id from Order o where o.id = :orderId")
  Optional<Long> findRestaurantIdById(@Param("orderId") Long orderId);

  @Query("select o.customer.id from Order o where o.id = :orderId")
  Optional<Long> findCustomerIdById(@Param("orderId") Long orderId);
}
