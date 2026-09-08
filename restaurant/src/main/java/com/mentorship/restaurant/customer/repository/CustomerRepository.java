package com.mentorship.restaurant.customer.repository;

import com.mentorship.restaurant.customer.model.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {}
