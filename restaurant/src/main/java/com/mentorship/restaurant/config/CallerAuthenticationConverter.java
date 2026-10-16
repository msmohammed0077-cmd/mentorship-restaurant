package com.mentorship.restaurant.config;

import com.mentorship.restaurant.customer.exception.EmailAlreadyInUseException;
import com.mentorship.restaurant.customer.service.CustomerService;
import com.mentorship.restaurant.user.model.Caller;
import com.mentorship.restaurant.user.service.UserService;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
 * here gets one on their first request if they signed up as a customer (ADR 0010): a verified
 * email, {@code customer.write} and no kitchen permission, through a sign-up client. Anyone else
 * unknown is refused with 401.
 */
@Component
public class CallerAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  /** The permission every sign-up holds: the realm's default role is CUSTOMER. */
  static final String CUSTOMER_SIGN_UP = "customer.write";

  /**
   * Permissions no customer holds. The default role also reaches staff created in the Keycloak
   * console, so a token with any of these is staff, linked by hand, never a sign-up.
   */
  static final Set<String> KITCHEN_PERMISSIONS =
      Set.of("order.accept", "order.reject", "order.prepare");

  private static final Logger log = LoggerFactory.getLogger(CallerAuthenticationConverter.class);

  private final UserService userService;
  private final CustomerService customerService;
  private final List<String> signUpClients;
  private final KeycloakPermissionsConverter permissions = new KeycloakPermissionsConverter();

  // Hand-written: Lombok drops @Value from generated constructor parameters.
  public CallerAuthenticationConverter(
      UserService userService,
      CustomerService customerService,
      @Value("${app.security.sign-up-clients}") List<String> signUpClients) {
    this.userService = userService;
    this.customerService = customerService;
    this.signUpClients = signUpClients;
  }

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    UUID keycloakId = keycloakIdOf(jwt);
    Collection<GrantedAuthority> authorities = permissions.convert(jwt);
    String verifiedEmail = verifiedEmailOf(jwt);
    Caller caller =
        lookUp(keycloakId, verifiedEmail)
            .orElseGet(() -> registerCustomer(keycloakId, jwt, verifiedEmail, authorities));
    return new CallerAuthenticationToken(caller, jwt, authorities);
  }

  private Optional<Caller> lookUp(UUID keycloakId, String verifiedEmail) {
    try {
      return userService.findCaller(keycloakId, verifiedEmail);
    } catch (DataIntegrityViolationException exception) {
      // Another request took the new email first; resolve without following it this time.
      log.warn("Could not follow the Keycloak email of {}", keycloakId, exception);
      return userService.findCaller(keycloakId, null);
    }
  }

  private Caller registerCustomer(
      UUID keycloakId, Jwt jwt, String verifiedEmail, Collection<GrantedAuthority> authorities) {
    if (verifiedEmail == null || !signedUpAsCustomer(authorities) || !fromSignUpClient(jwt)) {
      throw unknownUser();
    }
    String name = jwt.getClaimAsString("name");
    try {
      return customerService.registerFromToken(
          keycloakId, verifiedEmail, name == null || name.isBlank() ? verifiedEmail : name);
    } catch (EmailAlreadyInUseException exception) {
      throw new InvalidBearerTokenException("Email belongs to an account not linked to Keycloak");
    } catch (DataIntegrityViolationException exception) {
      // Usually two first requests raced and the other one committed; otherwise a 401, logged,
      // because an exception escaping the security filter is a raw 500 our error handler never
      // sees.
      return userService
          .findCaller(keycloakId, null)
          .orElseThrow(
              () -> {
                log.warn("Could not create the account for {}", keycloakId, exception);
                return unknownUser();
              });
    }
  }

  private static String verifiedEmailOf(Jwt jwt) {
    String email = jwt.getClaimAsString("email");
    boolean verified = Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"));
    return verified && email != null && !email.isBlank() ? email : null;
  }

  private static boolean signedUpAsCustomer(Collection<GrantedAuthority> authorities) {
    Set<String> held =
        authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    return held.contains(CUSTOMER_SIGN_UP)
        && held.stream().noneMatch(KITCHEN_PERMISSIONS::contains);
  }

  /** Only tokens issued to a client whose login offers registration may create accounts. */
  private boolean fromSignUpClient(Jwt jwt) {
    return signUpClients.contains(jwt.getClaimAsString("azp"));
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
