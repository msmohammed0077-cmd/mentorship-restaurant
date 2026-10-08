package com.mentorship.restaurant.customer.service;

import com.mentorship.restaurant.customer.exception.CustomerNotFoundException;
import com.mentorship.restaurant.customer.exception.EmailAlreadyInUseException;
import com.mentorship.restaurant.customer.exception.IncorrectPasswordException;
import com.mentorship.restaurant.customer.model.entity.Customer;
import com.mentorship.restaurant.customer.model.mapper.CustomerMapper;
import com.mentorship.restaurant.customer.model.request.ChangePasswordRequest;
import com.mentorship.restaurant.customer.model.request.CreateCustomerRequest;
import com.mentorship.restaurant.customer.model.request.UpdateCustomerRequest;
import com.mentorship.restaurant.customer.model.response.CustomerResponse;
import com.mentorship.restaurant.customer.repository.CustomerRepository;
import com.mentorship.restaurant.user.model.entity.User;
import com.mentorship.restaurant.user.service.UserService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

  private final CustomerRepository customerRepository;
  private final UserService userService;
  private final CustomerMapper customerMapper;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public CustomerResponse createCustomer(CreateCustomerRequest request) {
    String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

    ensureEmailAvailable(email);

    User user = new User();
    user.setUserName(request.getName());
    user.setUserEmail(email);
    user.setUserPassword(passwordEncoder.encode(request.getPassword()));
    user.setUserPhone(request.getPhone());
    user.setUserDateOfBirth(request.getDateOfBirth());
    user.setUserGender(request.getGender());

    Customer customer = new Customer();
    customer.setUser(userService.create(user));

    return customerMapper.toResponse(customerRepository.save(customer));
  }

  @Transactional(readOnly = true)
  public CustomerResponse getCustomer(Long customerId) {
    return customerMapper.toResponse(findActiveCustomer(customerId));
  }

  @Transactional
  public CustomerResponse updateCustomer(Long customerId, UpdateCustomerRequest request) {
    Customer customer = findActiveCustomer(customerId);
    User user = customer.getUser();

    if (request.getEmail() != null) {
      String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
      ensureEmailAvailableExcept(email, user.getId());
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

  @Transactional
  public void changePassword(Long customerId, ChangePasswordRequest request) {
    User user = findActiveCustomer(customerId).getUser();

    ensureCurrentPasswordMatches(request.getCurrentPassword(), user);

    // Managed entity: dirty checking issues the UPDATE at flush, no save() needed.
    user.setUserPassword(passwordEncoder.encode(request.getNewPassword()));
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

  private void ensureEmailAvailable(String email) {
    if (userService.isEmailTaken(email)) {
      throw new EmailAlreadyInUseException("Email is already in use");
    }
  }

  private void ensureEmailAvailableExcept(String email, Long userId) {
    if (userService.isEmailTakenByOther(email, userId)) {
      throw new EmailAlreadyInUseException("Email is already in use");
    }
  }

  private void ensureCurrentPasswordMatches(String currentPassword, User user) {
    if (!passwordEncoder.matches(currentPassword, user.getUserPassword())) {
      throw new IncorrectPasswordException("Current password is incorrect");
    }
  }
}
