package com.mentorship.restaurant.cart.infrastructure.persistence.repository;

import com.mentorship.restaurant.cart.infrastructure.persistence.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
}
