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

  Optional<CartItem> findByCart_IdAndMenuItem_Id(Long cartId, Long menuItemId);

  boolean existsByCart_IdAndMenuItem_Menu_Restaurant_IdNot(Long cartId, Long restaurantId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from CartItem cartItem where cartItem.cart.id = :cartId")
  int deleteAllByCart_Id(@Param("cartId") Long cartId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      delete from CartItem cartItem
      where cartItem.cart.id = :cartId and cartItem.id in :cartItemIds
      """)
  int deleteAllByCart_IdAndIdIn(
      @Param("cartId") Long cartId, @Param("cartItemIds") Collection<Long> cartItemIds);

  @Query(
      """
      select cartItem.id from CartItem cartItem
      where cartItem.cart.id = :cartId and cartItem.id in :cartItemIds
      """)
  List<Long> findIdsByCart_IdAndIdIn(
      @Param("cartId") Long cartId, @Param("cartItemIds") Collection<Long> cartItemIds);
}
