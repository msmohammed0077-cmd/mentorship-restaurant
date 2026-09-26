package com.mentorship.restaurant.customer.service.handler;

import com.mentorship.restaurant.customer.exception.AddressNotFoundException;
import com.mentorship.restaurant.customer.exception.CustomerNotFoundException;
import com.mentorship.restaurant.customer.model.entity.Address;
import com.mentorship.restaurant.customer.model.mapper.AddressMapper;
import com.mentorship.restaurant.customer.model.request.UpdateAddressRequest;
import com.mentorship.restaurant.customer.model.response.AddressResponse;
import com.mentorship.restaurant.customer.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateAddressHandler {

  private final AddressRepository addressRepository;
  private final AddressMapper addressMapper;

  @Transactional
  public AddressResponse updateAddress(
      Long customerId, Long addressId, UpdateAddressRequest request) {
    Address address =
        addressRepository
            .findByIdAndCustomerIdWithOwner(addressId, customerId)
            .orElseThrow(() -> new AddressNotFoundException("Address not found"));

    ensureOwnerNotDeleted(address);

    address.setLabel(request.getLabel());
    address.setLine(request.getLine());
    address.setCity(request.getCity());
    address.setArea(request.getArea());
    address.setNote(request.getNote());

    return addressMapper.toResponse(address);
  }

  /**
   * The lookup is scoped to the caller, so the owner is the caller: a soft-deleted one is a 404.
   */
  private void ensureOwnerNotDeleted(Address address) {
    if (address.getCustomer().getUser().getUserDeletedAt() != null) {
      throw new CustomerNotFoundException("Customer not found");
    }
  }
}
