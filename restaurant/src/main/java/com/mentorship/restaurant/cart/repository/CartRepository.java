package com.mentorship.restaurant.cart.repository;

import com.mentorship.restaurant.cart.model.entity.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {

  Optional<Cart> findByCustomer_Id(Long customerId);
}
