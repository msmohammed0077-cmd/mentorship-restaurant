package com.mentorship.restaurant.permission;

import java.util.Set;

/**
 * Answers "what may this user do?".
 *
 * <p>Keyed by <b>user</b> id, not customer id: a customer is one role a user plays, and the same
 * user may later also be a restaurant.
 *
 * <p>The intended end state is {@code user -> roles -> permissions}, resolved at login and cached
 * in Redis. None of that is built. A database-backed, cached implementation replaces the stub
 * behind this interface without any caller changing.
 */
public interface PermissionResolver {

  Set<String> permissionsOf(Long userId);
}
