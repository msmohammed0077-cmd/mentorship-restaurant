package com.mentorship.restaurant.restaurant.repository;

import com.mentorship.restaurant.restaurant.model.entity.MenuItem;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

  /**
   * A deleted item, an item of a deleted menu and a deleted restaurant's items do not exist to the
   * API, as the restaurant itself does not.
   */
  @Query(
      """
      select menuItem from MenuItem menuItem
      join menuItem.menu menu
      join menu.restaurant restaurant
      join restaurant.user user
      where menuItem.id = :menuItemId
        and menuItem.menuItemDeletedAt is null
        and menu.menuDeletedAt is null
        and user.userDeletedAt is null
      """)
  Optional<MenuItem> findActiveById(@Param("menuItemId") Long menuItemId);

  /** {@link #findActiveById} for several ids: checkout's and create-order's lines. */
  @Query(
      """
      select menuItem from MenuItem menuItem
      join menuItem.menu menu
      join menu.restaurant restaurant
      join restaurant.user user
      where menuItem.id in :menuItemIds
        and menuItem.menuItemDeletedAt is null
        and menu.menuDeletedAt is null
        and user.userDeletedAt is null
      """)
  List<MenuItem> findActiveByIdIn(@Param("menuItemIds") Collection<Long> menuItemIds);

  /** The item, if it is on that menu and not deleted. The menu is the caller's to check. */
  Optional<MenuItem> findByIdAndMenu_IdAndMenuItemDeletedAtIsNull(Long menuItemId, Long menuId);

  /** The menu's items for the read, available or not, in id order. */
  List<MenuItem> findByMenu_IdAndMenuItemDeletedAtIsNullOrderById(Long menuId);

  /** Every row, deleted or not, as the {@code menu_item_code} unique constraint does. */
  boolean existsByMenuItemCode(String menuItemCode);

  boolean existsByMenuItemCodeAndIdNot(String menuItemCode, Long menuItemId);

  /** Delete-menu's items, in one statement. */
  @Modifying(flushAutomatically = true)
  @Query(
      """
      update MenuItem menuItem set menuItem.menuItemDeletedAt = :now
      where menuItem.menu.id = :menuId and menuItem.menuItemDeletedAt is null
      """)
  void softDeleteAllOfMenu(@Param("menuId") Long menuId, @Param("now") OffsetDateTime now);

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
