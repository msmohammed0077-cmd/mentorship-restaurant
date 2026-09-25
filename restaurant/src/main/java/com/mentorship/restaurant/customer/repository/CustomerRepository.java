package com.mentorship.restaurant.customer.repository;

import com.mentorship.restaurant.customer.model.entity.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

  /** Excludes soft-deleted customers, like {@link #findActiveById}. */
  @Query(
      """
      select customer from Customer customer
      join customer.user user
      left join fetch customer.addresses
      where customer.id = :customerId and user.userDeletedAt is null
      """)
  Optional<Customer> findByIdWithAddresses(@Param("customerId") Long customerId);

  /**
   * Soft-deleted customers do not exist to the API. The user is join-fetched because {@code
   * CustomerMapper} reads it and open-in-view is off, so a LAZY proxy would fail after the
   * transaction.
   */
  @Query(
      """
      select customer from Customer customer
      join fetch customer.user user
      where customer.id = :customerId and user.userDeletedAt is null
      """)
  Optional<Customer> findActiveById(@Param("customerId") Long customerId);

  /** Existence check for handlers that need no entity; same soft-delete filter. */
  @Query(
      """
      select count(customer) > 0 from Customer customer
      where customer.id = :customerId and customer.user.userDeletedAt is null
      """)
  boolean existsActiveById(@Param("customerId") Long customerId);
}
