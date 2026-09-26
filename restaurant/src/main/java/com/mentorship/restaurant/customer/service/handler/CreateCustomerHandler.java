package com.mentorship.restaurant.customer.service.handler;

import com.mentorship.restaurant.customer.exception.EmailAlreadyInUseException;
import com.mentorship.restaurant.customer.model.entity.Customer;
import com.mentorship.restaurant.customer.model.entity.User;
import com.mentorship.restaurant.customer.model.mapper.CustomerMapper;
import com.mentorship.restaurant.customer.model.request.CreateCustomerRequest;
import com.mentorship.restaurant.customer.model.response.CustomerResponse;
import com.mentorship.restaurant.customer.repository.CustomerRepository;
import com.mentorship.restaurant.customer.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateCustomerHandler {

  private final UserRepository userRepository;
  private final CustomerRepository customerRepository;
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
    customer.setUser(userRepository.save(user));

    return customerMapper.toResponse(customerRepository.save(customer));
  }

  private void ensureEmailAvailable(String email) {
    if (userRepository.existsActiveByEmail(email)) {
      throw new EmailAlreadyInUseException("Email is already in use");
    }
  }
}
