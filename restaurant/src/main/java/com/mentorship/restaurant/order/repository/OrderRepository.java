package com.mentorship.restaurant.order.repository;

import com.mentorship.restaurant.order.model.entity.Order;
import com.mentorship.restaurant.order.model.entity.OrderStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

  /**
   * Moves the order only if it is still in the expected status. Zero rows means it was not — either
   * an illegal transition, a repeat of the current status, a terminal order, or someone moved it
   * first. All four are 409, and all four are indistinguishable here.
   *
   * <p>Reading the status and then writing it would be a race: two tabs both read ACCEPTED, both
   * transition, and the second write silently wins.
   */
  // flushAutomatically only. clearAutomatically would detach the whole
  // persistence context, which buys nothing here and is a trap for the tickets
  // that compensate in the same transaction as the transition.
  @Modifying(flushAutomatically = true)
  @Query(
      """
      update Order o set o.status = :to
      where o.id = :orderId and o.status = :from
      """)
  int updateStatusIfCurrent(
      @Param("orderId") Long orderId, @Param("from") OrderStatus from, @Param("to") OrderStatus to);

  /** Ownership and existence in one query, with no entity in scope to go stale. */
  @Query("select o.restaurant.id from Order o where o.id = :orderId")
  Optional<Long> findRestaurantIdById(@Param("orderId") Long orderId);
}
