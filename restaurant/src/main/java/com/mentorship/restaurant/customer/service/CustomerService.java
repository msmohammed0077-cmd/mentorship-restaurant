package com.mentorship.restaurant.customer.service;

import com.mentorship.restaurant.customer.model.request.CreateCustomerRequest;
import com.mentorship.restaurant.customer.model.response.CustomerResponse;
import com.mentorship.restaurant.customer.service.handler.CreateCustomerHandler;
import com.mentorship.restaurant.customer.service.handler.GetCustomerHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {

  private final CreateCustomerHandler createCustomerHandler;
  private final GetCustomerHandler getCustomerHandler;

  public CustomerResponse createCustomer(CreateCustomerRequest request) {
    return createCustomerHandler.createCustomer(request);
  }

  public CustomerResponse getCustomer(Long customerId) {
    return getCustomerHandler.getCustomer(customerId);
  }
}
