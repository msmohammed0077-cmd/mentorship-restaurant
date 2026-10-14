package com.mentorship.restaurant.menu.repository;

import com.mentorship.restaurant.menu.model.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {

}
