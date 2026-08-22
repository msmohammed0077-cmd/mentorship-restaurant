package com.mentorship.fooddelivery.cart.infrastructure.persistence.repository;

import com.mentorship.fooddelivery.cart.infrastructure.persistence.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
