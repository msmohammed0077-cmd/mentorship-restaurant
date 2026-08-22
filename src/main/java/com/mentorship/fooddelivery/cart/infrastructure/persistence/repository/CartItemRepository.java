package com.mentorship.fooddelivery.cart.infrastructure.persistence.repository;

import com.mentorship.fooddelivery.cart.infrastructure.persistence.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
