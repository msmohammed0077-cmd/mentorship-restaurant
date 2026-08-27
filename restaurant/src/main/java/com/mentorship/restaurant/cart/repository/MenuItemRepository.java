package com.mentorship.restaurant.cart.repository;

import com.mentorship.restaurant.cart.model.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {}
