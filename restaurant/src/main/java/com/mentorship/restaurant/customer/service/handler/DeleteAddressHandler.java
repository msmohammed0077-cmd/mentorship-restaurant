package com.mentorship.restaurant.customer.service.handler;

import com.mentorship.restaurant.customer.exception.AddressAccessDeniedException;
import com.mentorship.restaurant.customer.exception.AddressNotFoundException;
import com.mentorship.restaurant.customer.model.entity.Address;
import com.mentorship.restaurant.customer.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteAddressHandler {

  private final AddressRepository addressRepository;

  @Transactional
  public void deleteAddress(Long customerId, Long addressId) {
    Address address =
        addressRepository
            .findById(addressId)
            .orElseThrow(() -> new AddressNotFoundException("Address not found"));

    ensureAddressBelongsToCustomer(address, customerId);

    addressRepository.delete(address);
  }

  private void ensureAddressBelongsToCustomer(Address address, Long customerId) {
    if (!address.getCustomer().getId().equals(customerId)) {
      throw new AddressAccessDeniedException("Address belongs to another customer");
    }
  }
}
