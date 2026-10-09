package com.mentorship.restaurant.user.service;

import com.mentorship.restaurant.user.model.entity.User;
import com.mentorship.restaurant.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * The login account, for the domains that own one (customers today, restaurants later). Every
 * method joins the caller's transaction.
 */
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  /** Whether an active user holds the email, case-insensitively. */
  public boolean isEmailTaken(String email) {
    return userRepository.existsActiveByEmail(email);
  }

  /** {@link #isEmailTaken}, ignoring the given user, so re-sending your own email is not taken. */
  public boolean isEmailTakenByOther(String email, Long userId) {
    return userRepository.existsActiveByEmailAndIdNot(email, userId);
  }

  public User create(User user) {
    return userRepository.save(user);
  }
}
