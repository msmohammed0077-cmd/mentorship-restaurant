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

  Optional<Address> findByIdAndCustomer_Id(Long addressId, Long customerId);

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
