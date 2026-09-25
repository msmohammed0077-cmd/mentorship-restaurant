package com.mentorship.restaurant.customer.repository;

import com.mentorship.restaurant.customer.model.entity.PaymentMethod;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

  boolean existsByCustomer_Id(Long customerId);

  /**
   * A payment method of a soft-deleted customer does not exist to the API, so it is filtered here
   * rather than left for each handler to remember.
   */
  @Query(
      """
      select paymentMethod from PaymentMethod paymentMethod
      join paymentMethod.customer.user user
      where paymentMethod.id = :paymentMethodId and user.userDeletedAt is null
      """)
  Optional<PaymentMethod> findActiveById(@Param("paymentMethodId") Long paymentMethodId);

  @Query(
      """
      select paymentMethod from PaymentMethod paymentMethod
      where paymentMethod.customer.id = :customerId
      order by paymentMethod.isDefault desc, paymentMethod.createdAt desc, paymentMethod.id desc
      """)
  List<PaymentMethod> findAllByCustomerIdOrdered(@Param("customerId") Long customerId);

  @Modifying(flushAutomatically = true)
  @Query(
      """
      update PaymentMethod paymentMethod
      set paymentMethod.isDefault = false
      where paymentMethod.customer.id = :customerId and paymentMethod.isDefault = true
      """)
  int clearDefaultForCustomer(@Param("customerId") Long customerId);
}
