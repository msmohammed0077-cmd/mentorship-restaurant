package com.mentorship.restaurant.user.service;

import com.mentorship.restaurant.user.model.entity.User;
import com.mentorship.restaurant.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * The login account, for the domains that own one (customers and restaurants). Every method joins
 * the caller's transaction.
 */
@Service
@RequiredArgsConstructor
public class UserService {

  /**
   * The password of an account no one can log in to yet: a restaurant the admin created. It is not
   * a BCrypt hash, so no password matches it. Real authentication (#83) gives these accounts a real
   * password.
   */
  public static final String NO_LOGIN_PASSWORD = "!no-login";

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

  /**
   * Writes pending user changes now, so a unique-index violation surfaces in the caller's method,
   * where it can be answered, rather than at commit.
   */
  public void flush() {
    userRepository.flush();
  }
}
