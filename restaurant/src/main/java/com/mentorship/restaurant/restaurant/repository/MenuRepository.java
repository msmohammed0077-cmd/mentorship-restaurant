package com.mentorship.restaurant.restaurant.repository;

import com.mentorship.restaurant.restaurant.model.entity.Menu;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuRepository extends JpaRepository<Menu, Long> {

  /** The restaurant's menu, unless it or the restaurant is deleted. */
  @Query(
      """
      select menu from Menu menu
      join fetch menu.restaurant restaurant
      join restaurant.user user
      where restaurant.id = :restaurantId
        and menu.menuDeletedAt is null
        and user.userDeletedAt is null
      """)
  Optional<Menu> findActiveByRestaurantId(@Param("restaurantId") Long restaurantId);

  /** Matches the partial unique index {@code uq_menus_active_restaurant}. */
  boolean existsByRestaurant_IdAndMenuDeletedAtIsNull(Long restaurantId);

  /** Every row, deleted or not, as the {@code menu_code} unique constraint does. */
  boolean existsByMenuCode(String menuCode);

  boolean existsByMenuCodeAndIdNot(String menuCode, Long menuId);

  @Modifying(flushAutomatically = true)
  @Query("update Menu menu set menu.menuDeletedAt = :now where menu.id = :menuId")
  void softDelete(@Param("menuId") Long menuId, @Param("now") OffsetDateTime now);
}
