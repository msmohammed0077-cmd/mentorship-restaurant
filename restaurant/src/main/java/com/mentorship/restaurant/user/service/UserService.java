package com.mentorship.restaurant.user.service;

import com.mentorship.restaurant.user.model.Caller;
import com.mentorship.restaurant.user.model.CallerRow;
import com.mentorship.restaurant.user.model.entity.User;
import com.mentorship.restaurant.user.repository.UserRepository;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The login account, for the domains that own one (customers today, restaurants later). Every
 * method but {@link #findCaller} joins the caller's transaction.
 */
@Service
@RequiredArgsConstructor
public class UserService {

  private static final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;

  /** Whether an active user holds the email, case-insensitively. */
  public boolean isEmailTaken(String email) {
    return userRepository.existsActiveByEmail(email);
  }

  public User create(User user) {
    return userRepository.save(user);
  }

  /**
   * The caller behind a token's {@code sub}; empty when no users row links to it. Keycloak owns the
   * login email (ADR 0010), so a verified email that differs from ours replaces it, or a stale copy
   * would block whoever registers with the freed address. Runs from the security filter, outside
   * any use-case, so it opens its own transaction.
   *
   * @param verifiedEmail the token's email when Keycloak marks it verified, else null
   */
  @Transactional
  public Optional<Caller> findCaller(UUID keycloakId, String verifiedEmail) {
    return userRepository
        .findCallerByKeycloakId(keycloakId)
        .map(
            row -> {
              followKeycloakEmail(row, verifiedEmail);
              return row.caller();
            });
  }

  private void followKeycloakEmail(CallerRow row, String verifiedEmail) {
    if (verifiedEmail == null || row.deletedAt() != null) {
      return;
    }
    String email = verifiedEmail.trim().toLowerCase(Locale.ROOT);
    if (email.equalsIgnoreCase(row.email())) {
      return;
    }
    if (userRepository.existsActiveByEmail(email)) {
      // Another active row holds it, itself stale; it follows on its own owner's next request.
      log.warn("User {} keeps their old email: another account holds the new one", row.userId());
      return;
    }
    userRepository.updateEmail(row.userId(), email);
  }
}
