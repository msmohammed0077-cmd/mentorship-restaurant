package com.mentorship.restaurant.user.service;

import com.mentorship.restaurant.user.model.Caller;
import com.mentorship.restaurant.user.model.entity.User;
import com.mentorship.restaurant.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The login account, for the domains that own one (customers today, restaurants later). Every
 * method but {@link #findCaller} joins the caller's transaction.
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

  /**
   * The caller behind a token's {@code sub}; empty when no users row links to it. Runs from the
   * security filter, outside any use-case, so it opens its own read-only transaction.
   */
  @Transactional(readOnly = true)
  public Optional<Caller> findCaller(UUID keycloakId) {
    return userRepository.findCallerByKeycloakId(keycloakId);
  }
}
