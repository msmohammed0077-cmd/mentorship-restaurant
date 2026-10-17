package com.mentorship.restaurant.support;

import static com.mentorship.restaurant.config.KeycloakPermissionsConverter.PERMISSIONS_CLIENT;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * The role → permission matrix, read from the development realm file (ADR 0008). Tests sign tokens
 * with a role's permissions as Keycloak would expand it, so CI checks the realm's composites
 * without running Keycloak. Maven runs tests from {@code restaurant/}; an IDE may use the
 * repository root, so both are tried.
 */
public final class RealmPermissions {

  private static final Path REALM_FILE = locate(Path.of("").toAbsolutePath());
  private static final JsonNode REALM = new ObjectMapper().readTree(REALM_FILE);

  private RealmPermissions() {}

  /** The realm file under {@code workingDirectory}, or under its {@code restaurant/} directory. */
  static Path locate(Path workingDirectory) {
    List<Path> candidates =
        List.of(
            workingDirectory.resolve("keycloak").resolve("realm-restaurant.json"),
            workingDirectory
                .resolve("restaurant")
                .resolve("keycloak")
                .resolve("realm-restaurant.json"));
    return candidates.stream()
        .filter(Files::exists)
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "No keycloak/realm-restaurant.json under " + workingDirectory));
  }

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
