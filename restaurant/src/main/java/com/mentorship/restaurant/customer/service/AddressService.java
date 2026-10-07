package com.mentorship.restaurant.customer.service;

import com.mentorship.restaurant.customer.exception.AddressAccessDeniedException;
import com.mentorship.restaurant.customer.exception.AddressNotFoundException;
import com.mentorship.restaurant.customer.exception.CustomerNotFoundException;
import com.mentorship.restaurant.customer.model.entity.Address;
import com.mentorship.restaurant.customer.model.entity.Customer;
import com.mentorship.restaurant.customer.model.mapper.AddressMapper;
import com.mentorship.restaurant.customer.model.request.AddAddressRequest;
import com.mentorship.restaurant.customer.model.request.UpdateAddressRequest;
import com.mentorship.restaurant.customer.model.response.AddressResponse;
import com.mentorship.restaurant.customer.repository.AddressRepository;
import com.mentorship.restaurant.customer.repository.CustomerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddressService {

  private final AddressRepository addressRepository;
  private final CustomerRepository customerRepository;
  private final CustomerService customerService;
  private final AddressMapper addressMapper;

  @Transactional
  public AddressResponse addAddress(Long customerId, AddAddressRequest request) {
    Customer customer = customerService.findActiveCustomer(customerId);

    Address address = new Address();
    address.setCustomer(customer);
    address.setLabel(request.getLabel());
    address.setLine(request.getLine());
    address.setCity(request.getCity());
    address.setArea(request.getArea());
    address.setNote(request.getNote());
    address.setDefault(!addressRepository.existsByCustomer_Id(customerId));

    return addressMapper.toResponse(addressRepository.save(address));
  }

  @Transactional(readOnly = true)
  public List<AddressResponse> viewAddresses(Long customerId) {
    Customer customer =
        customerRepository
            .findByIdWithAddresses(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

    return addressMapper.toResponseList(customer.getAddresses());
  }

  @Transactional
  public AddressResponse updateAddress(
      Long customerId, Long addressId, UpdateAddressRequest request) {
    Address address = findCustomersAddress(addressId, customerId);
    // The lookup is scoped to the caller, so the owner is the caller: a soft-deleted one is a 404.
    customerService.ensureActive(address.getCustomer());

    address.setLabel(request.getLabel());
    address.setLine(request.getLine());
    address.setCity(request.getCity());
    address.setArea(request.getArea());
    address.setNote(request.getNote());

    return addressMapper.toResponse(address);
  }

  @Transactional
  public void deleteAddress(Long customerId, Long addressId) {
    Address address =
        addressRepository
            .findByIdWithOwner(addressId)
            .orElseThrow(() -> new AddressNotFoundException("Address not found"));
    ensureAddressBelongsToCustomer(address, customerId);
    // Runs after the ownership check, so the owner is the caller: a soft-deleted caller is a 404.
    customerService.ensureActive(address.getCustomer());

    addressRepository.delete(address);
  }

  @Transactional
  public AddressResponse setDefaultAddress(Long customerId, Long addressId) {
    Address address = findCustomersAddress(addressId, customerId);
    // The lookup is scoped to the caller, so the owner is the caller: a soft-deleted one is a 404.
    customerService.ensureActive(address.getCustomer());

    if (!address.isDefault()) {
      addressRepository.clearDefaultForCustomer(customerId);
      address.setDefault(true);
    }

    return addressMapper.toResponse(address);
  }

  private Address findCustomersAddress(Long addressId, Long customerId) {
    return addressRepository
        .findByIdAndCustomerIdWithOwner(addressId, customerId)
        .orElseThrow(() -> new AddressNotFoundException("Address not found"));
  }

  private void ensureAddressBelongsToCustomer(Address address, Long customerId) {
    if (!address.getCustomer().getId().equals(customerId)) {
      throw new AddressAccessDeniedException("Address belongs to another customer");
    }
  }
}
