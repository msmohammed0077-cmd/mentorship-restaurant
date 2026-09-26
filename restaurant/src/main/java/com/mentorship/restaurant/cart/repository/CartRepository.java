package com.mentorship.restaurant.cart.repository;

import com.mentorship.restaurant.cart.model.entity.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, Long> {

  Optional<Cart> findByCustomer_Id(Long customerId);

  /**
   * One statement; {@code cart_items} go with the cart by {@code ON DELETE CASCADE}. Without the
   * {@code @Query} a derived delete would load the cart and remove it entity by entity.
   */
  @Modifying(flushAutomatically = true)
  @Query("delete from Cart cart where cart.customer.id = :customerId")
  int deleteByCustomer_Id(@Param("customerId") Long customerId);
}
