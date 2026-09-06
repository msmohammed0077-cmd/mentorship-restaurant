package com.mentorship.restaurant.cart.repository;

import com.mentorship.restaurant.cart.model.entity.CartItem;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

  Optional<CartItem> findByIdAndCart_Id(Long id, Long cartId);

  /** Decides insert versus increment: the cart's existing line for this menu item, if any. */
  Optional<CartItem> findByCart_IdAndMenuItem_Id(Long cartId, Long menuItemId);

  /** True when the cart already holds a line from some other restaurant. */
  boolean existsByCart_IdAndMenuItem_Menu_Restaurant_IdNot(Long cartId, Long restaurantId);

  /**
   * Empties a cart in one statement. The explicit @Query is required, not stylistic: a derived
   * deleteAll... loads every row and deletes them one at a time, which is what this exists to
   * avoid.
   *
   * <p>A bulk delete bypasses the persistence context, so clearAutomatically detaches whatever it
   * has invalidated. Anything the caller needs afterwards must be read again.
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from CartItem cartItem where cartItem.cart.id = :cartId")
  int deleteAllByCart_Id(@Param("cartId") Long cartId);

  /** Scoped by cart as well as id, so one cart cannot delete another's items. */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      delete from CartItem cartItem
      where cartItem.cart.id = :cartId and cartItem.id in :cartItemIds
      """)
  int deleteAllByCart_IdAndIdIn(
      @Param("cartId") Long cartId, @Param("cartItemIds") Collection<Long> cartItemIds);

  /** Failure path only: names the ids that were actually present so the 404 can say which missed. */
  @Query(
      """
      select cartItem.id from CartItem cartItem
      where cartItem.cart.id = :cartId and cartItem.id in :cartItemIds
      """)
  List<Long> findIdsByCart_IdAndIdIn(
      @Param("cartId") Long cartId, @Param("cartItemIds") Collection<Long> cartItemIds);
}
