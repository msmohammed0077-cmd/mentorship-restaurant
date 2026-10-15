package com.mentorship.restaurant.config;

import com.mentorship.restaurant.user.model.Caller;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** A verified request: the resolved {@link Caller} as principal, its permissions as authorities. */
public class CallerAuthenticationToken extends AbstractAuthenticationToken {

  private final Caller caller;
  private final Jwt jwt;

  // Hand-written: the superclass takes the authorities.
  public CallerAuthenticationToken(
      Caller caller, Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.caller = caller;
    this.jwt = jwt;
    setAuthenticated(true);
  }

  @Override
  public Caller getPrincipal() {
    return caller;
  }

  @Override
  public Jwt getCredentials() {
    return jwt;
  }
}
