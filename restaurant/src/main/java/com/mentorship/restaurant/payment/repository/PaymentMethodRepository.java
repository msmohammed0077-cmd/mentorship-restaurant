package com.mentorship.restaurant.payment.repository;

import com.mentorship.restaurant.payment.model.entity.PaymentMethod;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

  boolean existsByCustomer_Id(Long customerId);

  /**
   * The caller's card with its owner and the owner's user, in one query. Another customer's card is
   * simply not found. Soft-deleted owners are not filtered: the service decides.
   */
  @Query(
      """
      select paymentMethod from PaymentMethod paymentMethod
      join fetch paymentMethod.customer customer
      join fetch customer.user
      where paymentMethod.id = :paymentMethodId and customer.id = :customerId
      """)
  Optional<PaymentMethod> findByIdAndCustomerIdWithOwner(
      @Param("paymentMethodId") Long paymentMethodId, @Param("customerId") Long customerId);

  @Modifying(flushAutomatically = true)
  @Query(
      """
      update PaymentMethod paymentMethod
      set paymentMethod.isDefault = false
      where paymentMethod.customer.id = :customerId and paymentMethod.isDefault = true
      """)
  int clearDefaultForCustomer(@Param("customerId") Long customerId);
}
