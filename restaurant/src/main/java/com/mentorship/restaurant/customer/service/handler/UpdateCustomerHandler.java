package com.mentorship.restaurant.customer.service.handler;

import com.mentorship.restaurant.customer.exception.CustomerNotFoundException;
import com.mentorship.restaurant.customer.exception.EmailAlreadyInUseException;
import com.mentorship.restaurant.customer.model.entity.Customer;
import com.mentorship.restaurant.customer.model.entity.User;
import com.mentorship.restaurant.customer.model.mapper.CustomerMapper;
import com.mentorship.restaurant.customer.model.request.UpdateCustomerRequest;
import com.mentorship.restaurant.customer.model.response.CustomerResponse;
import com.mentorship.restaurant.customer.repository.CustomerRepository;
import com.mentorship.restaurant.customer.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateCustomerHandler {

  private final CustomerRepository customerRepository;
  private final UserRepository userRepository;
  private final CustomerMapper customerMapper;

  @Transactional
  public CustomerResponse updateCustomer(Long customerId, UpdateCustomerRequest request) {
    Customer customer =
        customerRepository
            .findActiveById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));
    User user = customer.getUser();

    if (request.getEmail() != null) {
      String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
      ensureEmailAvailable(email, user.getId());
      user.setUserEmail(email);
    }
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

  private void ensureEmailAvailable(String email, Long userId) {
    if (userRepository.existsActiveByEmailAndIdNot(email, userId)) {
      throw new EmailAlreadyInUseException("Email is already in use");
    }
  }
}
