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
   * One query for the payment-method list: an empty result means the customer is unknown or
   * soft-deleted; otherwise the cards come back in the order {@code Customer.paymentMethods}
   * declares.
   */
  @Query(
      """
      select customer from Customer customer
      join fetch customer.user user
      left join fetch customer.paymentMethods
      where customer.id = :customerId and user.userDeletedAt is null
      """)
  Optional<Customer> findByIdWithPaymentMethods(@Param("customerId") Long customerId);

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
