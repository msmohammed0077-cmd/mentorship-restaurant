package com.mentorship.restaurant.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The caller's permissions: the client roles Keycloak lists in {@code
 * resource_access.restaurant-api.roles}, with composites already expanded (ADR 0008). Realm roles
 * are ignored; the API never checks a role. A token missing any level of the claim has no
 * permissions, so every protected endpoint refuses it with 403.
 */
public class KeycloakPermissionsConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  /** The client whose roles are the API's permissions; also the audience it checks. */
  public static final String PERMISSIONS_CLIENT = "restaurant-api";

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    if (!(jwt.getClaims().get("resource_access") instanceof Map<?, ?> resourceAccess)
        || !(resourceAccess.get(PERMISSIONS_CLIENT) instanceof Map<?, ?> client)
        || !(client.get("roles") instanceof Collection<?> roles)) {
      return List.of();
    }
    return roles.stream()
        .map(Object::toString)
        .<GrantedAuthority>map(SimpleGrantedAuthority::new)
        .toList();
  }
}
