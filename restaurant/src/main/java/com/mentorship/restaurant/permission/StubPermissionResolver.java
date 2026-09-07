package com.mentorship.restaurant.permission;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Stands in for RBAC until it exists. A user absent from the configuration holds nothing, so the
 * configuration never has to enumerate everyone — and that absence is what makes the 403 path
 * reachable without a fixture of its own.
 */
@Service
@RequiredArgsConstructor
public class StubPermissionResolver implements PermissionResolver {

  private final PermissionProperties properties;

  @Override
  public Set<String> permissionsOf(Long userId) {
    List<String> codes = properties.getPermissions().get(userId);
    return codes == null ? Set.of() : Set.copyOf(codes);
  }
}
