package com.mentorship.restaurant.customer.service.handler;

import com.mentorship.restaurant.customer.exception.CustomerNotFoundException;
import com.mentorship.restaurant.customer.model.entity.Customer;
import com.mentorship.restaurant.customer.model.mapper.CustomerMapper;
import com.mentorship.restaurant.customer.model.response.CustomerResponse;
import com.mentorship.restaurant.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCustomerHandler {

  private final CustomerRepository customerRepository;
  private final CustomerMapper customerMapper;

  @Transactional(readOnly = true)
  public CustomerResponse getCustomer(Long customerId) {
    Customer customer =
        customerRepository
            .findActiveById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

    return customerMapper.toResponse(customer);
  }
}
