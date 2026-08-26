package com.mentorship.restaurant.cart.repository;

import com.mentorship.restaurant.cart.model.entity.CartItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

  Optional<CartItem> findByIdAndCart_Id(Long id, Long cartId);
}
