package com.mentorship.restaurant.customer.service;

import com.mentorship.restaurant.customer.exception.CustomerNotFoundException;
import com.mentorship.restaurant.customer.model.entity.Customer;
import com.mentorship.restaurant.customer.model.mapper.CustomerMapper;
import com.mentorship.restaurant.customer.model.request.UpdateCustomerRequest;
import com.mentorship.restaurant.customer.model.response.CustomerResponse;
import com.mentorship.restaurant.customer.repository.CustomerRepository;
import com.mentorship.restaurant.user.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

  private final CustomerRepository customerRepository;
  private final CustomerMapper customerMapper;

  @Transactional(readOnly = true)
  public CustomerResponse getCustomer(Long customerId) {
    return customerMapper.toResponse(findActiveCustomer(customerId));
  }

  @Transactional
  public CustomerResponse updateCustomer(Long customerId, UpdateCustomerRequest request) {
    Customer customer = findActiveCustomer(customerId);
    User user = customer.getUser();

    if (request.getName() != null) {
      user.setUserName(request.getName());
    }
    if (request.getPhone() != null) {
      user.setUserPhone(request.getPhone());
    }
    if (request.getDateOfBirth() != null) {
      user.setUserDateOfBirth(request.getDateOfBirth());
    }
    if (request.getGender() != null) {
      user.setUserGender(request.getGender());
    }

    // Managed entity: dirty checking issues the UPDATE at flush, no save() needed.
    return customerMapper.toResponse(customer);
  }

  /**
   * The active customer, else a 404. Shared with {@link AddressService} and with the cart, payment
   * and delete-customer services; joins the caller's transaction.
   */
  public Customer findActiveCustomer(Long customerId) {
    return customerRepository
        .findActiveById(customerId)
        .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
  }

  /** {@link #findActiveCustomer} for callers that need no entity: one count query, same 404. */
  public void ensureActiveCustomerExists(Long customerId) {
    if (!customerRepository.existsActiveById(customerId)) {
      throw new CustomerNotFoundException("Customer not found");
    }
  }

  /**
   * A soft-deleted customer does not exist to the API, so acting on their behalf is a 404. Callers
   * run it after their ownership check, so the customer is the caller.
   */
  public void ensureActive(Customer customer) {
    if (customer.getUser().getUserDeletedAt() != null) {
      throw new CustomerNotFoundException("Customer not found");
    }
  }
}
