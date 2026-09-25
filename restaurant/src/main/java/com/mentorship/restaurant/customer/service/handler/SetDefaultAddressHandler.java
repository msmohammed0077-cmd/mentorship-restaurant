package com.mentorship.restaurant.customer.service.handler;

import com.mentorship.restaurant.customer.exception.AddressNotFoundException;
import com.mentorship.restaurant.customer.exception.CustomerNotFoundException;
import com.mentorship.restaurant.customer.model.entity.Address;
import com.mentorship.restaurant.customer.model.mapper.AddressMapper;
import com.mentorship.restaurant.customer.model.response.AddressResponse;
import com.mentorship.restaurant.customer.repository.AddressRepository;
import com.mentorship.restaurant.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SetDefaultAddressHandler {

  private final CustomerRepository customerRepository;
  private final AddressRepository addressRepository;
  private final AddressMapper addressMapper;

  @Transactional
  public AddressResponse setDefaultAddress(Long customerId, Long addressId) {
    ensureCustomerExists(customerId);

    Address address =
        addressRepository
            .findByIdAndCustomer_Id(addressId, customerId)
            .orElseThrow(() -> new AddressNotFoundException("Address not found"));

    if (!address.isDefault()) {
      addressRepository.clearDefaultForCustomer(customerId);
      address.setDefault(true);
    }

    return addressMapper.toResponse(address);
  }

  private void ensureCustomerExists(Long customerId) {
    if (!customerRepository.existsActiveById(customerId)) {
      throw new CustomerNotFoundException("Customer not found");
    }
  }
}
