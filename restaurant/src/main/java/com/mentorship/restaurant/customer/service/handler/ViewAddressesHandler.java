package com.mentorship.restaurant.customer.service.handler;

import com.mentorship.restaurant.customer.exception.CustomerNotFoundException;
import com.mentorship.restaurant.customer.model.entity.Customer;
import com.mentorship.restaurant.customer.model.mapper.AddressMapper;
import com.mentorship.restaurant.customer.model.response.AddressResponse;
import com.mentorship.restaurant.customer.repository.CustomerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ViewAddressesHandler {

  private final CustomerRepository customerRepository;
  private final AddressMapper addressMapper;

  @Transactional(readOnly = true)
  public List<AddressResponse> viewAddresses(Long customerId) {
    Customer customer =
        customerRepository
            .findByIdWithAddresses(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

    return addressMapper.toResponseList(customer.getAddresses());
  }
}
