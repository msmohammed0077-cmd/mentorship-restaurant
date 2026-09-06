package com.mentorship.restaurant.customer.repository;

import com.mentorship.restaurant.customer.model.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {

  boolean existsByCustomer_Id(Long customerId);
}
