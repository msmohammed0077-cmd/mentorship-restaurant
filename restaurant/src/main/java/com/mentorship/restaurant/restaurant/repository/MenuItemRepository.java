package com.mentorship.restaurant.restaurant.repository;

import com.mentorship.restaurant.restaurant.model.entity.MenuItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

  /** A deleted restaurant's items do not exist to the API, as the restaurant itself does not. */
  @Query(
      """
      select menuItem from MenuItem menuItem
      join menuItem.menu menu
      join menu.restaurant restaurant
      join restaurant.user user
      where menuItem.id = :menuItemId and user.userDeletedAt is null
      """)
  Optional<MenuItem> findActiveById(@Param("menuItemId") Long menuItemId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      update MenuItem menuItem
      set menuItem.stock = menuItem.stock - :quantity
      where menuItem.id = :menuItemId and menuItem.stock >= :quantity
      """)
  int decrementStockIfAvailable(
      @Param("menuItemId") Long menuItemId, @Param("quantity") Integer quantity);

  @Modifying(flushAutomatically = true)
  @Query(
      """
      update MenuItem menuItem
      set menuItem.stock = menuItem.stock + :quantity
      where menuItem.id = :menuItemId
      """)
  int incrementStock(@Param("menuItemId") Long menuItemId, @Param("quantity") Integer quantity);
}
