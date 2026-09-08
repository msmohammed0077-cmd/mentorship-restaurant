package com.mentorship.restaurant.order.repository;

import com.mentorship.restaurant.order.model.entity.Order;
import com.mentorship.restaurant.order.model.entity.OrderStatus;
import com.mentorship.restaurant.order.model.entity.RejectionReason;
import java.time.OffsetDateTime;
import java.util.List;
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

  @Modifying(flushAutomatically = true)
  @Query("update Order o set o.prepTimeMinutes = :minutes where o.id = :orderId")
  int updatePrepTime(@Param("orderId") Long orderId, @Param("minutes") Integer minutes);

  @Modifying(flushAutomatically = true)
  @Query(
      """
      update Order o set o.rejectionReason = :reason, o.rejectionNote = :note
      where o.id = :orderId
      """)
  int updateRejectionReason(
      @Param("orderId") Long orderId,
      @Param("reason") RejectionReason reason,
      @Param("note") String note);

  @Query(
      """
      select o.id from Order o
      where o.status = com.mentorship.restaurant.order.model.entity.OrderStatus.PLACED
        and o.createdAt < :deadline
      """)
  List<Long> findStalePlacedOrderIds(@Param("deadline") OffsetDateTime deadline);
}
