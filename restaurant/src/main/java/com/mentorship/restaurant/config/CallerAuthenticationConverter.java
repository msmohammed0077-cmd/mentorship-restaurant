package com.mentorship.restaurant.config;

import com.mentorship.restaurant.customer.exception.EmailAlreadyInUseException;
import com.mentorship.restaurant.customer.service.CustomerService;
import com.mentorship.restaurant.user.model.Caller;
import com.mentorship.restaurant.user.service.UserService;
import java.util.Collection;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

/**
 * Turns a verified token into the request's caller (ADR 0009): {@code sub} → users → customers,
 * plus the permissions {@link KeycloakPermissionsConverter} reads. A Keycloak user with no account
 * here gets one on their first request if they signed up as a customer — a verified email and
 * {@code customer.write} (ADR 0010); anyone else unknown is refused with 401.
 */
@Component
@RequiredArgsConstructor
public class CallerAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  /** The permission that marks a sign-up as a customer: every sign-up gets CUSTOMER. */
  static final String CUSTOMER_SIGN_UP = "customer.write";

  private final UserService userService;
  private final CustomerService customerService;
  private final KeycloakPermissionsConverter permissions = new KeycloakPermissionsConverter();

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    UUID keycloakId = keycloakIdOf(jwt);
    Collection<GrantedAuthority> authorities = permissions.convert(jwt);
    Caller caller =
        userService
            .findCaller(keycloakId)
            .orElseGet(() -> registerCustomer(keycloakId, jwt, authorities));
    return new CallerAuthenticationToken(caller, jwt, authorities);
  }

  private Caller registerCustomer(
      UUID keycloakId, Jwt jwt, Collection<GrantedAuthority> authorities) {
    String email = jwt.getClaimAsString("email");
    if (!signedUpAsCustomer(authorities)
        || !Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))
        || email == null
        || email.isBlank()) {
      throw unknownUser();
    }
    String name = jwt.getClaimAsString("name");
    try {
      return customerService.registerFromToken(
          keycloakId, email, name == null || name.isBlank() ? email : name);
    } catch (EmailAlreadyInUseException exception) {
      throw unknownUser();
    } catch (DataIntegrityViolationException exception) {
      // Two first requests raced; the other one created the rows and committed.
      return userService.findCaller(keycloakId).orElseThrow(() -> exception);
    }
  }

  private static boolean signedUpAsCustomer(Collection<GrantedAuthority> authorities) {
    return authorities.stream()
        .anyMatch(authority -> CUSTOMER_SIGN_UP.equals(authority.getAuthority()));
  }

  private static UUID keycloakIdOf(Jwt jwt) {
    try {
      return UUID.fromString(jwt.getSubject());
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw unknownUser();
    }
  }

  private static InvalidBearerTokenException unknownUser() {
    return new InvalidBearerTokenException("Unknown user");
  }
}
