package com.mentorship.restaurant.order.repository;

import com.mentorship.restaurant.order.model.entity.Order;
import com.mentorship.restaurant.order.model.entity.OrderStatus;
import com.mentorship.restaurant.order.model.entity.RejectionReason;
import com.mentorship.restaurant.order.model.response.OrderSummaryResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
  String SELECT_SUMMARY =
      """
      select new com.mentorship.restaurant.order.model.response.OrderSummaryResponse(
        o.id,
        cast(o.status as string),
        o.restaurant.restaurantName,
        (select count(oi) from OrderItem oi where oi.order = o),
        o.total,
        o.createdAt)
      from Order o
      where o.customer.id = :customerId
      """;

  String NEWEST_FIRST = " order by o.createdAt desc, o.id desc";

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

  @Query(SELECT_SUMMARY + NEWEST_FIRST)
  List<OrderSummaryResponse> findFirstPage(@Param("customerId") Long customerId, Pageable pageable);

  @Query(
      SELECT_SUMMARY
          + " and (o.createdAt < :cursorCreatedAt"
          + " or (o.createdAt = :cursorCreatedAt and o.id < :cursorId))"
          + NEWEST_FIRST)
  List<OrderSummaryResponse> findPageAfter(
      @Param("customerId") Long customerId,
      @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
      @Param("cursorId") Long cursorId,
      Pageable pageable);
}
