package com.mentorship.restaurant.cart.repository;

import com.mentorship.restaurant.cart.model.entity.CartItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

  Optional<CartItem> findByIdAndCart_Id(Long id, Long cartId);

  /** Decides insert versus increment: the cart's existing line for this menu item, if any. */
  Optional<CartItem> findByCart_IdAndMenuItem_Id(Long cartId, Long menuItemId);
}
