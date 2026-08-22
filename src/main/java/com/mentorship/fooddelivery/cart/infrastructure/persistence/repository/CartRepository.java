package com.mentorship.fooddelivery.cart.infrastructure.persistence.repository;

import com.mentorship.fooddelivery.cart.infrastructure.persistence.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
}
