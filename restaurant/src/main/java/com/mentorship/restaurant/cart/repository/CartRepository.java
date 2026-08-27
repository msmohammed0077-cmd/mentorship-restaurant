package com.mentorship.restaurant.cart.repository;

import com.mentorship.restaurant.cart.model.entity.Cart;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CartRepository extends JpaRepository<Cart, Long> {

  Optional<Cart> findByCustomer_Id(Long customerId);

  /**
   * The cart's restaurant is not stored. It is read from the cart's lines, and an empty cart
   * returns no rows, which is what lets it accept an item from any restaurant.
   */
  @Query(
      """
      select distinct m.restaurant.id
      from CartItem ci
      join ci.menuItem mi
      join mi.menu m
      where ci.cart.id = :cartId
      """)
  List<Long> findRestaurantIdsByCartId(Long cartId);
}
