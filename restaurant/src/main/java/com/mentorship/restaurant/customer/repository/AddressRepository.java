package com.mentorship.restaurant.customer.repository;

import com.mentorship.restaurant.customer.model.entity.Address;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AddressRepository extends JpaRepository<Address, Long> {

  boolean existsByCustomer_Id(Long customerId);

  /**
   * The caller's address with its owner and the owner's user, in one query. Another customer's
   * address is simply not found. Soft-deleted owners are not filtered: the handler decides what
   * that means for the caller.
   */
  @Query(
      """
      select address from Address address
      join fetch address.customer customer
      join fetch customer.user
      where address.id = :addressId and customer.id = :customerId
      """)
  Optional<Address> findByIdAndCustomerIdWithOwner(
      @Param("addressId") Long addressId, @Param("customerId") Long customerId);

  /**
   * Any customer's address with its owner and the owner's user, in one query, so a handler can
   * check ownership and the owner's soft delete without a second round trip. Soft-deleted owners
   * are not filtered.
   */
  @Query(
      """
      select address from Address address
      join fetch address.customer customer
      join fetch customer.user
      where address.id = :addressId
      """)
  Optional<Address> findByIdWithOwner(@Param("addressId") Long addressId);

  @Query(
      """
      select address from Address address
      where address.customer.id = :customerId
      order by address.isDefault desc, address.createdAt desc
      """)
  List<Address> findAllByCustomerIdOrdered(@Param("customerId") Long customerId);

  @Modifying(flushAutomatically = true)
  @Query(
      """
      update Address address
      set address.isDefault = false
      where address.customer.id = :customerId and address.isDefault = true
      """)
  int clearDefaultForCustomer(@Param("customerId") Long customerId);
}
