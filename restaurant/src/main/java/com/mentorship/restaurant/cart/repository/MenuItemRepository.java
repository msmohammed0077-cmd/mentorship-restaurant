package com.mentorship.restaurant.cart.repository;

import com.mentorship.restaurant.cart.model.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

  /**
   * Decrements stock only when the current quantity is sufficient.
   *
   * <p>Clearing the persistence context detaches whatever the caller was holding, so callers must
   * take what they need from their entities before the first call and re-read anything they need
   * afterwards.
   *
   * @return the number of rows updated, which is 1 when the decrement succeeds
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      update MenuItem menuItem
      set menuItem.stock = menuItem.stock - :quantity
      where menuItem.id = :menuItemId and menuItem.stock >= :quantity
      """)
  int decrementStockIfAvailable(
      @Param("menuItemId") Long menuItemId, @Param("quantity") Integer quantity);
}
