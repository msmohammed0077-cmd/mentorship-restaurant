package com.mentorship.restaurant.customer.repository;

import com.mentorship.restaurant.customer.model.entity.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

  @Query(
      """
      select customer from Customer customer
      left join fetch customer.addresses
      where customer.id = :customerId
      """)
  Optional<Customer> findByIdWithAddresses(@Param("customerId") Long customerId);
}
