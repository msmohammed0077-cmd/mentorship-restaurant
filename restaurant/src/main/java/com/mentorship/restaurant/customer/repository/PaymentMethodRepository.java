package com.mentorship.restaurant.customer.repository;

import com.mentorship.restaurant.customer.model.entity.PaymentMethod;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

  boolean existsByCustomer_Id(Long customerId);

  /**
   * Loads the card with its owner and the owner's user in one query, so a handler can check
   * ownership and the owner's soft delete without a second round trip. It does not filter
   * soft-deleted owners: the handler decides what that means for the caller.
   */
  @Query(
      """
      select paymentMethod from PaymentMethod paymentMethod
      join fetch paymentMethod.customer customer
      join fetch customer.user
      where paymentMethod.id = :paymentMethodId
      """)
  Optional<PaymentMethod> findByIdWithOwner(@Param("paymentMethodId") Long paymentMethodId);

  @Modifying(flushAutomatically = true)
  @Query(
      """
      update PaymentMethod paymentMethod
      set paymentMethod.isDefault = false
      where paymentMethod.customer.id = :customerId and paymentMethod.isDefault = true
      """)
  int clearDefaultForCustomer(@Param("customerId") Long customerId);
}
