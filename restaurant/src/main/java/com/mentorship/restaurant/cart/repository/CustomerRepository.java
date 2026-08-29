package com.mentorship.restaurant.cart.repository;

import com.mentorship.restaurant.cart.model.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {}
