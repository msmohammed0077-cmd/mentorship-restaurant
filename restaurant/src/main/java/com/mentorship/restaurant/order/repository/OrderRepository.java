package com.mentorship.restaurant.order.repository;

import com.mentorship.restaurant.order.model.entity.Order;
import com.mentorship.restaurant.order.model.entity.OrderStatus;
import com.mentorship.restaurant.order.model.entity.RejectionReason;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
  boolean existsByCustomer_IdAndStatusIn(Long customerId, Collection<OrderStatus> statuses);

  boolean existsByRestaurant_IdAndStatusIn(Long restaurantId, Collection<OrderStatus> statuses);

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

  /**
   * The order's customer id and that customer's soft-delete timestamp, in one query. A projection
   * rather than an entity, because the status transition that follows is a bulk update and would
   * leave a loaded order stale. Soft-deleted owners are not filtered.
   */
  @Query(
      """
      select customer.id as customerId, user.userDeletedAt as customerDeletedAt
      from Order o join o.customer customer join customer.user user
      where o.id = :orderId
      """)
  Optional<OrderOwnerProjection> findOwnerById(@Param("orderId") Long orderId);

  /**
   * Everything rate-order checks, in one query: the owner, the owner's soft delete, the status, and
   * whether a rating exists. Soft-deleted owners are not filtered; the service decides.
   */
  @Query(
      """
      select customer.id as customerId, user.userDeletedAt as customerDeletedAt,
             o.status as status, case when rating.id is null then false else true end as rated
      from Order o join o.customer customer join customer.user user
      left join OrderRating rating on rating.order = o
      where o.id = :orderId
      """)
  Optional<OrderRatingContextProjection> findRatingContextById(@Param("orderId") Long orderId);

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

  /**
   * A customer's orders, newest first, one keyset window at a time. Derived rather than
   * {@code @Query} because Spring Data scrolls only derived queries. The graph loads the restaurant
   * for the summary, and the transaction: an eager inverse one-to-one that Hibernate would
   * otherwise fetch with one SELECT per order.
   */
  @EntityGraph(attributePaths = {"restaurant", "transaction"})
  Window<Order> findByCustomer_IdOrderByCreatedAtDescIdDesc(
      Long customerId, ScrollPosition position, Limit limit);
}
