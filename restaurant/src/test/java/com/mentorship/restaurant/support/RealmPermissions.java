package com.mentorship.restaurant.support;

import static com.mentorship.restaurant.config.KeycloakPermissionsConverter.PERMISSIONS_CLIENT;

import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The role → permission matrix, read from the development realm file (ADR 0008). Tests sign tokens
 * with a role's permissions as Keycloak would expand it, so CI checks the realm's composites
 * without running Keycloak. Tests run from {@code restaurant/}.
 */
public final class RealmPermissions {

  private static final Path REALM_FILE = Path.of("keycloak", "realm-restaurant.json");
  private static final JsonNode REALM = new ObjectMapper().readTree(REALM_FILE);

  private RealmPermissions() {}

  /** The permissions {@code realmRole} is a composite of. */
  public static Set<String> of(String realmRole) {
    for (JsonNode role : REALM.path("roles").path("realm")) {
      if (role.path("name").asString().equals(realmRole)) {
        Set<String> permissions = new TreeSet<>();
        role.path("composites")
            .path("client")
            .path(PERMISSIONS_CLIENT)
            .forEach(permission -> permissions.add(permission.asString()));
        return permissions;
      }
    }
    throw new IllegalArgumentException("No realm role " + realmRole + " in " + REALM_FILE);
  }

  /** Every permission: the client roles of {@code restaurant-api}. */
  public static Set<String> all() {
    Set<String> permissions = new TreeSet<>();
    REALM
        .path("roles")
        .path("client")
        .path(PERMISSIONS_CLIENT)
        .forEach(permission -> permissions.add(permission.path("name").asString()));
    return permissions;
  }
}
