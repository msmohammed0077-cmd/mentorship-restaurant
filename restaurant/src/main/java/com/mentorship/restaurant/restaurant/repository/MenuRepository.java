package com.mentorship.restaurant.restaurant.repository;

import com.mentorship.restaurant.restaurant.model.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {

}
