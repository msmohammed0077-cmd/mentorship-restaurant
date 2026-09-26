package com.mentorship.restaurant.customer.service.handler;

import com.mentorship.restaurant.customer.exception.CustomerNotFoundException;
import com.mentorship.restaurant.customer.exception.IncorrectPasswordException;
import com.mentorship.restaurant.customer.model.entity.User;
import com.mentorship.restaurant.customer.model.request.ChangePasswordRequest;
import com.mentorship.restaurant.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangePasswordHandler {

  private final CustomerRepository customerRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public void changePassword(Long customerId, ChangePasswordRequest request) {
    User user =
        customerRepository
            .findActiveById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found"))
            .getUser();

    ensureCurrentPasswordMatches(request.getCurrentPassword(), user);

    // Managed entity: dirty checking issues the UPDATE at flush, no save() needed.
    user.setUserPassword(passwordEncoder.encode(request.getNewPassword()));
  }

  private void ensureCurrentPasswordMatches(String currentPassword, User user) {
    if (!passwordEncoder.matches(currentPassword, user.getUserPassword())) {
      throw new IncorrectPasswordException("Current password is incorrect");
    }
  }
}
