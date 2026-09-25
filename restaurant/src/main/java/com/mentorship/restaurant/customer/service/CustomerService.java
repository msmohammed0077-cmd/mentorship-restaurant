package com.mentorship.restaurant.customer.service;

import com.mentorship.restaurant.customer.model.request.ChangePasswordRequest;
import com.mentorship.restaurant.customer.model.request.CreateCustomerRequest;
import com.mentorship.restaurant.customer.model.request.UpdateCustomerRequest;
import com.mentorship.restaurant.customer.model.response.CustomerResponse;
import com.mentorship.restaurant.customer.service.handler.ChangePasswordHandler;
import com.mentorship.restaurant.customer.service.handler.CreateCustomerHandler;
import com.mentorship.restaurant.customer.service.handler.DeleteCustomerHandler;
import com.mentorship.restaurant.customer.service.handler.GetCustomerHandler;
import com.mentorship.restaurant.customer.service.handler.UpdateCustomerHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {

  private final CreateCustomerHandler createCustomerHandler;
  private final GetCustomerHandler getCustomerHandler;
  private final UpdateCustomerHandler updateCustomerHandler;
  private final ChangePasswordHandler changePasswordHandler;
  private final DeleteCustomerHandler deleteCustomerHandler;

  public CustomerResponse createCustomer(CreateCustomerRequest request) {
    return createCustomerHandler.createCustomer(request);
  }

  public CustomerResponse getCustomer(Long customerId) {
    return getCustomerHandler.getCustomer(customerId);
  }

  public CustomerResponse updateCustomer(Long customerId, UpdateCustomerRequest request) {
    return updateCustomerHandler.updateCustomer(customerId, request);
  }

  public void changePassword(Long customerId, ChangePasswordRequest request) {
    changePasswordHandler.changePassword(customerId, request);
  }

  public void deleteCustomer(Long customerId) {
    deleteCustomerHandler.deleteCustomer(customerId);
  }
}
