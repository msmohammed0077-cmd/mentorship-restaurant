package com.mentorship.restaurant.config;

import com.mentorship.restaurant.user.model.Caller;
import com.mentorship.restaurant.user.service.UserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

/**
 * Turns a verified token into the request's caller (ADR 0009): {@code sub} → users → customers,
 * plus the permissions {@link KeycloakPermissionsConverter} reads. A token whose user has no
 * account here is refused with 401.
 */
@Component
@RequiredArgsConstructor
public class CallerAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private final UserService userService;
  private final KeycloakPermissionsConverter permissions = new KeycloakPermissionsConverter();

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    Caller caller =
        userService
            .findCaller(keycloakIdOf(jwt))
            .orElseThrow(() -> new InvalidBearerTokenException("Unknown user"));
    return new CallerAuthenticationToken(caller, jwt, permissions.convert(jwt));
  }

  private static UUID keycloakIdOf(Jwt jwt) {
    try {
      return UUID.fromString(jwt.getSubject());
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new InvalidBearerTokenException("Unknown user");
    }
  }
}
